import asyncio
import json
from datetime import datetime
from typing import List, Dict, Callable, Awaitable, Any

import aiohttp
from aiohttp import ClientSession

from api.metrics.data.model import MetricInstance, RawMetric, Metric, Collect, Resource, ResourceMax, ResourceSet
from api.metrics.data.table import select_metric_instances, select_metrics, create_metric_table, \
    USAGE_METRIC_NAMES, insert_metrics
from api.settings.data.model import SettingType
from api.settings.data.table import select_settings, upsert_setting
from api.settings.svc.postgresql import last_postgresql_info, get_postgresql_instance
from api.settings.svc.prometheus import get_all_prometheus_apis
from core.db_postgresql import Postgresql
from core.globals import bus, BusEvents

PROMETHEUS_API_URI = "/api/v1/query?query="

PROMQL_USAGE_AVG = {
    'cpu_usage': '100-(avg(rate(node_cpu_seconds_total{mode="idle"}[1m]))by(instance)*100)',
    'mem_usage': 'sum((node_memory_MemTotal_bytes-node_memory_MemAvailable_bytes)/node_memory_MemTotal_bytes*100)by(instance)',
    'dsk_usage': 'sum(100-((node_filesystem_avail_bytes{mountpoint="/",fstype!="rootfs"}*100)/node_filesystem_size_bytes{mountpoint="/",fstype!="rootfs"}))by(instance)',
    'gpu_usage': 'avg_over_time(DCGM_FI_DEV_GPU_UTIL[1m])',
    'dsk_io': 'sum(instance_device:node_disk_io_time_seconds:rate5m/scalar(count(instance_device:node_disk_io_time_seconds:rate5m)))by(instance)'
}

PROMQL_USAGE_AVL = {
    'cpu_avail': '(sum(count(node_cpu_seconds_total{mode="idle"})without(cpu,mode))by(instance)*1000)*((avg(rate(node_cpu_seconds_total{mode="idle"}[1m]))by(instance)*100)*0.01)',
    'mem_avail': 'sum(node_memory_MemAvailable_bytes)by(instance)',
    'dsk_avail': 'sum(node_filesystem_avail_bytes)by(instance)',
}

PROMQL_PER_POD = {
    'cpu': 'sum(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate)by(namespace,node,pod)',
    'mem': 'sum(container_memory_rss{job="kubelet",metrics_path="/metrics/cadvisor",container!=""})by(namespace,node,pod)'
}

PROMQL_MAX = {
    'cpu': 'sum(count(node_cpu_seconds_total{mode="idle"})without(cpu,mode))by(instance)',
    'mem': 'sum(node_memory_MemTotal_bytes)by(instance)',
    'dsk': 'sum(node_filesystem_size_bytes{mountpoint="/",fstype!="rootfs"})by(instance)',
}

# 수집 시작/종료
collecting = Collect(start=False)

# 인스턴스
postgresql = Postgresql()


@bus.on(BusEvents.POSTGRESQL_INFO_UPDATED)
def set_postgresql():
    if last_postgresql_info.is_valid():
        global postgresql
        postgresql = get_postgresql_instance()
        asyncio.create_task(create_metric_table(postgresql))
        asyncio.create_task(ready())


async def ready():
    _collecting = await is_collecting()
    if _collecting:
        await collect_start(_collecting.value == 'True')


async def collect_start(start: bool) -> bool:
    if start:
        apis = await get_all_prometheus_apis()
        if len(apis) == 0 or not postgresql.db:
            return False

    setattr(collecting, 'start', start)
    await _update_collecting(start)
    return True


async def _update_collecting(start: bool):
    await upsert_setting(SettingType.COLLECTING, str(start))


async def is_collecting():
    settings = await select_settings(SettingType.COLLECTING)
    if len(settings) > 0:
        return settings[0]
    return False


# 저장된 Prometheus API와 Postgresql 정보로 메트릭 수집
async def collect_with_api():
    if collecting.start and postgresql.db:
        await get_data_with_promql_wrapper(PROMQL_USAGE_AVG | PROMQL_USAGE_AVL, _parse_metrics_and_insert)


async def get_data_with_promql_wrapper(queries: Dict[str, str],
                                       callback: Callable[[ClientSession, str, str, datetime], Awaitable[Any]],
                                       reducer: Callable[[Any], Any] = None):
    """

    Args:
        queries: e.g. PROMQL_…
        callback: e.g. lambda results, metric_name, prom_url: Do something
        reducer:
    Returns:

    """
    apis = await get_all_prometheus_apis()
    prom_urls = list(map(lambda api: api.value, apis))
    await get_data_with_promql(prom_urls, queries, callback, reducer)


async def get_data_with_promql(prom_urls: List[str],
                               queries: Dict[str, str],
                               callback: Callable[[ClientSession, str, str, datetime], Awaitable[Any]],
                               reducer: Callable[[Any], Any] = None):
    now = datetime.now()

    async with aiohttp.ClientSession() as session:
        for metric_name, query in queries.items():
            for prom_url in prom_urls:
                async with session.get(prom_url + PROMETHEUS_API_URI + query) as response:
                    text = await response.text()
                    body = json.loads(text)
                    results = body['data']['result']
                    callback_result = await callback(results, metric_name, prom_url, now)
                    if reducer:
                        reducer(callback_result)


async def _parse_metrics_and_insert(results: Any, metric_name: str, prometheus_url: str, now: datetime):
    metrics = []
    for result in results:
        if 'metric' in result:
            instance = result['metric']['instance']
            value = result['value'][1]
            metric = RawMetric(metric_name=metric_name, instance=instance, value=value, prometheus_url=prometheus_url)
            metrics.append(metric)
    await insert_metrics(postgresql, metrics, now)


#

async def get_latest_instances() -> List[MetricInstance]:
    if postgresql.db:
        instances = await select_metric_instances(postgresql)
        return instances
    return []


def new_metric_obj(metric_names: List[str], row):
    metric = Metric()
    metric.date_created = row[0]
    for i, metric_name in enumerate(metric_names):
        setattr(metric, metric_name, row[i + 1])
    return metric


def default_filter(row):
    return not (row[1] is None or row[2] is None or row[3] is None)


async def get_monitoring_data(instance: str, prometheus_url: str,
                              steps=4, with_forecast=True,
                              hours: int = 1, minutes: int = 0,
                              metric_names: List[str] = USAGE_METRIC_NAMES,
                              filter_func=default_filter) -> List[Metric]:
    if postgresql.db:
        rows = await select_metrics(postgresql, instance, prometheus_url, hours, minutes, metric_names)
        rows = filter(filter_func, rows)
        metrics: List[Metric] = list(map(lambda x: new_metric_obj(metric_names, x), rows))
        return metrics
        # if len(metrics) > 0 and with_forecast:
        #     try:
        #         forecasts = forecast(metrics, steps)
        #         return metrics + forecasts
        #     except ValueError:
        #         return metrics
    return []


async def get_resources(prom_urls: List[str] = None) -> ResourceSet:
    resources: List[Resource] = []
    max_resources: List[ResourceMax] = []
    if prom_urls is None:
        await get_data_with_promql_wrapper(PROMQL_PER_POD, _parse_resource, lambda res: resources.extend(res))
        await get_data_with_promql_wrapper(PROMQL_MAX, _parse_resource_max, lambda res: max_resources.extend(res))
    else:
        await get_data_with_promql(prom_urls, PROMQL_PER_POD, _parse_resource, lambda res: resources.extend(res))
        await get_data_with_promql(prom_urls, PROMQL_MAX, _parse_resource_max, lambda res: max_resources.extend(res))
    return ResourceSet(value=resources, max=max_resources)


async def _parse_resource(results: Any, metric_name, prom_url, now):
    resources = []
    for result in results:
        if 'metric' in result:
            metric = result['metric']
            namespace = metric['namespace'] if 'namespace' in metric else ''
            node = metric['node']
            pod = metric['pod'] if 'pod' in metric else 'etc'
            value = result['value'][1]
            resources.append(Resource(prom_url=prom_url, data_type=metric_name, namespace=namespace,
                                      node=node, pod=pod, value=value))
    return resources


async def _parse_resource_max(results: Any, metric_name, prom_url, now):
    resources = []
    for result in results:
        if 'metric' in result:
            node = result['metric']['instance']
            value = result['value'][1]
            resources.append(ResourceMax(prom_url=prom_url, data_type=metric_name, node=node, value=value))
    return resources


def set_resource_max(max_resources: List[Resource], resources: List[Resource]):
    for max_resource in max_resources:
        for resource in resources:
            if resource.prom_url == max_resource.prom_url and \
                    resource.node == max_resource.node and \
                    resource.data_type == max_resource.data_type:
                resource.max = max_resource.value
