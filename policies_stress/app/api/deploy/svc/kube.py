# 배포
import asyncio
import itertools
import json
import traceback
from typing import List, Tuple

import pandas as pd
from hikaru import load_full_yaml, HikaruDocumentBase, get_yaml
from hikaru.model.rel_1_26 import PodSpec
from kubernetes import config, client, utils
from kubernetes.client import V1NodeList, V1ObjectMeta, V1Namespace

from api.deploy.data.model import Result, ResourceQuota, ResourceMini, InstanceMini, HikaruMini
from api.deploy.svc.filter import parse_cpu_core, find_cpu_max, create_temp_file, parse_json
from api.instances.data.table import select_instances
from api.kube.data.model import KubeResponseDTO
from api.kube.data.table import select_kube_configs, select_temp
from api.metrics.data.model import ResourceSet
from api.metrics.svc.collect import get_resources, get_monitoring_data
from api.settings.svc.postgresql import last_postgresql_info, get_postgresql_instance
from core.db_postgresql import Postgresql
from core.globals import bus, BusEvents

postgresql = Postgresql()

kube_configs: List[KubeResponseDTO]
loaded_yaml: List[HikaruDocumentBase]
stress_quotas: List[ResourceQuota]
deploy_quotas: List[ResourceQuota]

COMPLETED: str = json.dumps({'complete': 'true'}, default=str)


@bus.on(BusEvents.POSTGRESQL_INFO_UPDATED)
def set_postgresql():
    if last_postgresql_info.is_valid():
        global postgresql
        postgresql = get_postgresql_instance()


async def deploy_cpu():
    return generator_chain(
        show_valid_configs, show_cpu_test_app_yaml,
        apply_policies, deploy_stress_app
    )


async def generator_chain(*await_funcs):
    try:
        for func in await_funcs:
            generator = func()
            has_err_msg = False
            async for item in generator:
                if isinstance(item, Result):
                    yield item.to_json()
                    if has_err_msg:
                        return
                    elif not item.success:
                        if item.size > 0:
                            has_err_msg = True
                        else:
                            return
                else:
                    yield item
                await asyncio.sleep(.5)
    finally:
        await asyncio.sleep(.5)
        yield COMPLETED


def remove_yaml_and_to_json(kube_config: KubeResponseDTO):
    return json.dumps(kube_config.without_yaml().__dict__, default=str)


async def show_valid_configs():
    global kube_configs
    kube_configs = await select_kube_configs(postgresql, config_required=True, config_slicing=False)

    if len(kube_configs) == 0:
        yield Result.fail()
    else:
        temp = await select_temp(postgresql, "cpu_test_app")
        cpu_test_app_yaml = temp.value

        bases: List[HikaruDocumentBase] = load_full_yaml(yaml=cpu_test_app_yaml)
        if len(bases) == 0:
            yield Result.fail()
        else:
            base = bases[0]

            kind_entries = base.find_by_name('kind')
            kind: str = base.object_at_path(kind_entries[0].path)

            meta_entries = base.find_by_name('metadata')
            meta: V1ObjectMeta = base.object_at_path(meta_entries[0].path)

            if kind == 'Namespace':
                namespace = meta.name
            else:
                namespace = meta.namespace

            still_terminating = False
            for kube_config in kube_configs:
                config_file = create_temp_file(kube_config.config)
                config.load_kube_config(config_file)

                cli = client.CoreV1Api()
                try:
                    ns: V1Namespace = cli.read_namespace(namespace)
                    if ns.status.phase == 'Terminating':
                        still_terminating = True
                except (BaseException,):
                    # namespace not exists
                    pass

            if still_terminating:
                yield Result.fail(1)
                yield '이전 테스트 초기화가 아직 진행 중입니다.'
            else:
                json_configs = list(map(remove_yaml_and_to_json, kube_configs))
                yield Result.success('json', size=len(json_configs))
                for json_config in json_configs:
                    yield json_config


async def show_cpu_test_app_yaml():
    temp = await select_temp(postgresql, "cpu_test_app")
    cpu_test_app_yaml = temp.value

    try:
        global loaded_yaml
        loaded_yaml = load_full_yaml(yaml=cpu_test_app_yaml)
        yield Result.success(size=1)
        yield cpu_test_app_yaml
    except (BaseException,):
        yield Result.fail()


async def get_resource_quotas() -> List[ResourceQuota]:
    resource_quotas: List[ResourceQuota] = []

    for kube_config in kube_configs:
        resources: ResourceSet = await get_resources([kube_config.prometheus_url])
        cpu_values = list(filter(lambda r: r.data_type == 'cpu', resources.value))

        df = pd.DataFrame({
            'prom_url': [value.prom_url for value in cpu_values],
            'node': [value.node for value in cpu_values],
            'value': [float(value.value) for value in cpu_values],
        })
        df_nodes = df.groupby(['prom_url', 'node']).sum()

        # 마스터 노드 제외
        config_file = create_temp_file(kube_config.config)
        config.load_kube_config(config_file)

        v1 = client.CoreV1Api()
        node_list: V1NodeList = v1.list_node()
        worker_nodes = []
        for node in node_list.items:
            is_master = False
            for taint in node.spec.taints or []:
                if taint.key == 'node-role.kubernetes.io/master' and taint.effect == 'NoSchedule':
                    is_master = True
            if not is_master:
                worker_nodes.append({'node': node.metadata.name})

        df_workers = pd.DataFrame.from_records(worker_nodes, index=['node'])
        df_merged = pd.merge(df_nodes, df_workers, on=['node'])
        group_by_node = df_merged.reset_index().agg(lambda x: x.tolist(), axis=1).tolist()

        for node in group_by_node:
            cpu_usage = ResourceMini(prom_url=kube_config.prometheus_url, node=node[0], value=node[1])
            cpu_max = find_cpu_max(resources.max, cpu_usage)
            cpu_remain = parse_cpu_core(str(float(cpu_max.value) - cpu_usage.value))

            for app in loaded_yaml:

                entries = app.find_by_name("spec")
                for entry in entries:
                    obj = app.object_at_path(entry.path)
                    if type(obj) == PodSpec:
                        spec: PodSpec = obj

                        for container in spec.containers:
                            requests = container.resources.requests.get('cpu')
                            limits = container.resources.limits.get('cpu')
                            quota = ResourceQuota(prom_url=cpu_usage.prom_url,
                                                  node=cpu_usage.node,
                                                  name=container.name,
                                                  requests=parse_cpu_core(requests),
                                                  limits=parse_cpu_core(limits),
                                                  remain=round(cpu_remain, 3))
                            if quota.requests < quota.remain:
                                resource_quotas.append(quota)
    return resource_quotas


async def apply_priority_and_merge_instance_resource(resource_quotas) -> (pd.DataFrame, List[ResourceQuota]):
    instances = await select_instances(postgresql)
    instances = list(map(lambda inst: InstanceMini(prom_url=inst.prometheus_url,
                                                   node=inst.instance_name,
                                                   sort_order=inst.sort_order), instances))
    df_sort = pd.DataFrame(instances)
    df_quota = pd.DataFrame(resource_quotas)
    df_sorted: pd.DataFrame = pd.merge(df_quota, df_sort, on=['prom_url', 'node']).sort_values('sort_order')

    sorted_list = df_sorted.reset_index().agg(lambda x: x.tolist(), axis=1).tolist()
    sorted_list = list(map(lambda x: ResourceQuota(
        prom_url=x[1], node=x[2], name=x[3],
        requests=x[4], limits=x[5], remain=x[6]), sorted_list))

    return df_sorted, sorted_list


async def apply_policies():
    try:
        resource_quotas = await get_resource_quotas()
        if len(resource_quotas) == 0:
            yield Result.fail(1)
            yield '자원 상태를 만족하는 노드가 없습니다.'

        yield Result.success(size=len(resource_quotas))
        for quota in resource_quotas:
            # 자원 상태를 만족하는 노드
            yield "{0} ({1})\n{2}: {3}/{4} ~ {5}".format(quota.node, quota.prom_url, quota.name,
                                                         quota.requests, quota.remain, quota.limits)

        # 노드 우선순위 적용
        df_sorted, sorted_list = await apply_priority_and_merge_instance_resource(resource_quotas)
        yield Result.success(size=1)
        yield df_sorted.to_html()

        global stress_quotas
        stress_quotas = sorted_list

    except (BaseException,):
        tb = traceback.format_exc()
        print(tb)
        yield Result.fail()


async def deploy_stress_app():
    try:
        if len(stress_quotas) > 0:
            stress_node = stress_quotas[0]
            prom_configs = list(filter(lambda conf: conf.prometheus_url == stress_node.prom_url, kube_configs))
            if len(prom_configs) > 0:
                print('>> deploy stress app')
                print('prometheus_url:', stress_node.prom_url)
                print('node:', stress_node.node)

                config_file = create_temp_file(prom_configs[0].config)
                config.load_kube_config(config_file)

                temp = await select_temp(postgresql, "cpu_stress_app")
                cpu_stress_app_yaml = temp.value

                success = await deploy_from_yaml(cpu_stress_app_yaml, stress_node.node)
                if success:
                    yield Result.success('json', size=1)
                    yield parse_json(stress_node)
                else:
                    yield Result.fail(1)
                    yield '이미 배포되었습니다.'
            else:
                yield Result.fail()
    except (BaseException,):
        yield Result.fail()


# 15초마다 사용량 그래프 갱신, cpu 사용량이 90% 넘어가면 다음 단계로
async def check_cpu_over_90_pct():
    stress_node = stress_quotas[0]
    metrics = await get_monitoring_data(stress_node.node, stress_node.prom_url,
                                        hours=0, minutes=1, filter_func=None,
                                        metric_names=['cpu_usage'])
    metrics = sorted(metrics, key=lambda x: x.date_created, reverse=True)
    if len(metrics) > 0:
        usage = float(metrics[0].cpu_usage)
        return usage > 90
    else:
        return False


async def deploy_cpu2():
    return generator_chain(
        check_excluded, deploy_test_app, delete_all_apps
    )


# 분산 정책 적용한 노드들 중 1순위 노드가 제외되었는지 확인
async def check_excluded():
    try:
        global stress_quotas
        stress_node = stress_quotas[0]

        global kube_configs
        kube_configs = await select_kube_configs(postgresql, config_required=True, config_slicing=False)

        temp = await select_temp(postgresql, "cpu_test_app")
        cpu_test_app_yaml = temp.value

        global loaded_yaml
        loaded_yaml = load_full_yaml(yaml=cpu_test_app_yaml)

        global deploy_quotas
        resource_quotas = await get_resource_quotas()
        _, deploy_quotas = await apply_priority_and_merge_instance_resource(resource_quotas)

        find_nodes = list(
            filter(lambda x: x.prom_url == stress_node.prom_url and x.node == stress_node.node, deploy_quotas))
        if len(find_nodes) > 0:
            yield Result.fail(1)
            yield '1순위 노드가 그대로 남아있습니다.'
        else:
            yield Result.success(size=1)
            yield '정책에 따라 1순위 노드인 {0}가 제외되었습니다.\n프로메테우스 URL: {1}'.format(stress_node.node, stress_node.prom_url)

    except (BaseException,):
        tb = traceback.format_exc()
        print(tb)
        yield Result.fail(1)
        yield '잘못된 접근입니다.'


# cpu 테스트 앱 배포
async def deploy_test_app():
    try:
        if len(deploy_quotas) > 0:
            deploy_node = deploy_quotas[0]
            prom_configs = list(filter(lambda conf: conf.prometheus_url == deploy_node.prom_url, kube_configs))
            if len(prom_configs) > 0:
                print('>> deploy test app')
                print('prometheus_url:', deploy_node.prom_url)
                print('node:', deploy_node.node)

                config_file = create_temp_file(prom_configs[0].config)
                config.load_kube_config(config_file)

                temp = await select_temp(postgresql, "cpu_test_app")
                cpu_test_app_yaml = temp.value

                success = await deploy_from_yaml(cpu_test_app_yaml, deploy_node.node)
                if success:
                    yield Result.success('json', size=1)
                    yield parse_json(deploy_node)
                else:
                    yield Result.fail(1)
                    yield '이미 배포되었습니다.'
            else:
                yield Result.fail()
    except (BaseException,):
        yield Result.fail()


async def deploy_from_yaml(yaml_str: str, node_name: str) -> bool:
    bases: List[HikaruDocumentBase] = load_full_yaml(yaml=yaml_str)
    exists = 0
    for base in bases:
        entries = base.find_by_name("spec")
        for entry in entries:
            obj = base.object_at_path(entry.path)
            if type(obj) == PodSpec:
                spec: PodSpec = obj
                spec.nodeSelector.update({'kubernetes.io/hostname': node_name})

        yaml = get_yaml(base)
        yaml_file = create_temp_file(yaml)

        try:
            cli = client.ApiClient()
            utils.create_from_yaml(cli, yaml_file)
            print('deployed yaml:\n', yaml)
        except utils.FailToCreateError as e:
            info = json.loads(e.api_exceptions[0].body)
            if info.get('reason').lower() == 'alreadyexists':
                exists += 1
            tb = traceback.format_exc()
            print(tb)

    return exists != len(bases)


async def delete_app(config_text: str, app_yaml: str, prom_url: str, node_name: str) -> Tuple[
    List[str], List[HikaruMini]]:
    config_file = create_temp_file(config_text)
    config.load_kube_config(config_file)

    cli = client.CoreV1Api()
    app = client.AppsV1Api()

    tasks: List[str] = []
    items: List[HikaruMini] = []

    bases: List[HikaruDocumentBase] = load_full_yaml(yaml=app_yaml)

    for base in bases:
        kind_entries = base.find_by_name("kind")
        kind: str = base.object_at_path(kind_entries[0].path)

        if kind == 'Namespace':
            name_entries = base.find_by_name('name')
            name: str = base.object_at_path(name_entries[0].path)
            items.append(HikaruMini(kind=kind, name=name))
        elif kind == 'Deployment':
            meta_entries = base.find_by_name('metadata')
            meta: V1ObjectMeta = base.object_at_path(meta_entries[0].path)
            items.append(HikaruMini(kind=kind, name=meta.name, namespace=meta.namespace))

    items = sorted(items, key=lambda i: i.kind)
    for item in items:
        # for t, _ in itertools.groupby(items, lambda i: (i.kind, i.name, i.namespace)):
        #     item = HikaruMini(kind=t[0], name=t[1], namespace=t[2])
        try:
            print('try to delete {0}, {1}, {2}'.format(item.kind, item.name, item.namespace))
            if item.kind == 'Namespace':
                cli.delete_namespace(item.name)
                tasks.append('Namespace: {0}의 삭제를 요청했습니다.\n({1}: {2})'.format(item.name, node_name, prom_url))
            elif item.kind == 'Deployment':
                app.delete_namespaced_deployment(item.name, item.namespace)
                tasks.append('Deployment: {0}의 삭제를 요청했습니다.\n({1}: {2})'.format(item.name, node_name, prom_url))
        except (BaseException,):
            tb = traceback.format_exc()
            print(tb)

    return tasks, items


async def delete_all_apps():
    global kube_configs
    kube_configs = await select_kube_configs(postgresql, config_required=True, config_slicing=False)

    global stress_quotas
    stress_node = stress_quotas[0]
    stress_configs = list(filter(lambda conf: conf.prometheus_url == stress_node.prom_url, kube_configs))

    print('>> delete all apps')

    all_tasks: List[str] = []

    if len(stress_configs) > 0:
        print('prometheus_url:', stress_node.prom_url)
        print('node:', stress_node.node)

        temp = await select_temp(postgresql, "cpu_stress_app")
        cpu_stress_app_yaml = temp.value

        tasks, items = await delete_app(stress_configs[0].config, cpu_stress_app_yaml, stress_node.prom_url,
                                        stress_node.node)
        all_tasks = all_tasks + tasks

    global deploy_quotas
    deploy_node = deploy_quotas[0]
    deploy_configs = list(filter(lambda conf: conf.prometheus_url == deploy_node.prom_url, kube_configs))

    if len(deploy_configs) > 0:
        print('prometheus_url:', deploy_node.prom_url)
        print('node:', deploy_node.node)

        temp = await select_temp(postgresql, "cpu_test_app")
        cpu_test_app_yaml = temp.value

        tasks, items = await delete_app(deploy_configs[0].config, cpu_test_app_yaml, deploy_node.prom_url,
                                        deploy_node.node)
        all_tasks = all_tasks + tasks

    yield Result.success(size=len(all_tasks))
    for task in all_tasks:
        yield task
