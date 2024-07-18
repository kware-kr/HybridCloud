from typing import List

from api.instances.data.model import Instance, InstanceDto
from core.db_postgresql import Postgresql

QUERY_INSTANCE_TABLE = """
CREATE TABLE IF NOT EXISTS INSTANCE
(
    instance_name  text PRIMARY KEY,
    prometheus_url text PRIMARY KEY,
    display_name   text,
    enabled        text DEFAULT 'true',
    sort_order     INTEGER DEFAULT 0,
    date_created   TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
)"""

QUERY_INSTANCE_SELECT = """
SELECT instance_name, prometheus_url, display_name, enabled, sort_order, date_created
  FROM instance
"""

QUERY_INSTANCE_INSERT = """
INSERT INTO instance (instance_name, prometheus_url)
VALUES ('{0}', '{1}')
"""

QUERY_INSTANCE_UPDATE = """
UPDATE instance 
   SET enabled = '{2}'
     , display_name = '{3}'
     , sort_order = '{4}'
 WHERE instance_name = '{0}'
   AND prometheus_url = '{1}'
"""

QUERY_INSTANCE_MULT_UPDATE = """
UPDATE instance a
   SET sort_order = b.sort_order
  FROM (VALUES {0}) AS b(instance_name, prometheus_url, sort_order)
 WHERE b.instance_name = a.instance_name
   AND b.prometheus_url = a.prometheus_url
"""

QUERY_INSTANCE_DELETE = "DELETE FROM instance WHERE prometheus_url = '{0}'"

QUERY_INSTANCE_MULT_UPDATE_OBJ = "('{0}', '{1}', {2})"


async def create_instance_table(postgresql: Postgresql):
    await postgresql.execute(QUERY_INSTANCE_TABLE)


async def select_instances(postgresql: Postgresql) -> List[InstanceDto]:
    rows = await postgresql.fetch_all(QUERY_INSTANCE_SELECT)
    instances = list(map(lambda x: InstanceDto(
        instance_name=x[0],
        prometheus_url=x[1],
        display_name=x[2] or '',
        enabled=x[3],
        sort_order=x[4],
        date_created=x[5],
    ), rows))
    return instances


async def insert_instance(postgresql: Postgresql, instance: Instance):
    query = QUERY_INSTANCE_INSERT.format(instance.instance_name, instance.prometheus_url)
    await postgresql.execute(query)


async def update_instance(postgresql: Postgresql, instance: Instance):
    query = QUERY_INSTANCE_UPDATE.format(instance.instance_name, instance.prometheus_url,
                                         instance.enabled, instance.display_name, instance.sort_order)
    await postgresql.execute(query)


async def update_instances_sort_order(postgresql: Postgresql, instances: List[Instance]):
    items = []
    for instance in instances:
        item_query = QUERY_INSTANCE_MULT_UPDATE_OBJ.format(instance.instance_name, instance.prometheus_url,
                                                           instance.sort_order)
        items.append(item_query)
    query = QUERY_INSTANCE_MULT_UPDATE.format(', '.join(items))
    await postgresql.execute(query)


async def delete_instances(postgresql: Postgresql, prometheus_url: str):
    query = QUERY_INSTANCE_DELETE.format(prometheus_url)
    await postgresql.execute(query)
