import asyncio
from typing import List

from psycopg2.errors import UniqueViolation

from api.instances.data.model import Instance, InstanceDto
from api.instances.data.table import create_instance_table, select_instances, insert_instance, update_instance, \
    update_instances_sort_order
from api.metrics.data.model import MetricInstance
from api.metrics.data.table import select_metric_instances
from api.settings.svc.postgresql import last_postgresql_info, get_postgresql_instance
from core.db_postgresql import Postgresql
from core.globals import bus, BusEvents

postgresql = Postgresql()


@bus.on(BusEvents.POSTGRESQL_INFO_UPDATED)
def set_postgresql():
    if last_postgresql_info.is_valid():
        global postgresql
        postgresql = get_postgresql_instance()
        asyncio.create_task(create_instance_table(postgresql))


async def get_instances() -> List[InstanceDto]:
    if postgresql.db:
        instances = await select_instances(postgresql)
        return instances
    return []


async def modi_instance(instance: Instance):
    if postgresql.db:
        await update_instance(postgresql, instance)


async def refresh_and_get_instances() -> List[InstanceDto]:
    if postgresql.db:
        metric_instances: List[MetricInstance] = await select_metric_instances(postgresql)
        metric_instances.sort(key=lambda x: (x.prometheus_url, x.instance))

        instances = list(map(lambda x: InstanceDto(
            instance_name=x.instance,
            prometheus_url=x.prometheus_url,
        ), metric_instances))
        for instance in instances:
            try:
                await insert_instance(postgresql, instance)
            except UniqueViolation:
                pass

        instances: List[InstanceDto] = await select_instances(postgresql)
        return instances
    return []


async def modi_instances_sort_order(instances: List[Instance]):
    if postgresql.db:
        await update_instances_sort_order(postgresql, instances)
