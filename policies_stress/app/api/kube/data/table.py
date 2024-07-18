from typing import List

from api.kube.data.model import KubeResponseDTO, KubeRequestDTO, TempData
from core.db_postgresql import Postgresql

QUERY_KUBE_TABLE = """
CREATE TABLE IF NOT EXISTS kubernetes
(
    prometheus_url text NOT NULL PRIMARY KEY,
    display_name   text NOT NULL,
    config         text,
    date_created   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    date_updated   TIMESTAMP WITHOUT TIME ZONE
)"""

QUERY_KUBE_INSERT = """
INSERT INTO kubernetes (prometheus_url, display_name)
VALUES ('{0}', '{1}')
"""

QUERY_KUBE_UPDATE = """
UPDATE kubernetes
   SET display_name   = '{1}'{2}
     , date_updated   = NOW()
 WHERE prometheus_url = '{0}'
"""

QUERY_KUBE_UPDATE_FILE = "\n     , config         = '{0}'"

QUERY_KUBE_SELECT = """
SELECT prometheus_url, display_name, config, date_created, date_updated
  FROM kubernetes{0}
"""

QUERY_KUBE_SELECT_HAS_CONFIG = "\n WHERE config is not null"

QUERY_TEMP_TABLE = """
CREATE TABLE IF NOT EXISTS temp
(
    NAME         text NOT NULL PRIMARY KEY,
    VALUE        text,
    date_created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    date_updated TIMESTAMP WITHOUT TIME ZONE
)"""

QUERY_TEMP_SELECT = "SELECT value, date_created, date_updated FROM temp WHERE name = '{0}'"


async def create_kube_config_and_temp_table(postgresql: Postgresql):
    await postgresql.execute(QUERY_KUBE_TABLE)
    await postgresql.execute(QUERY_TEMP_TABLE)


async def insert_kube_config(postgresql: Postgresql, dto: KubeRequestDTO):
    query = QUERY_KUBE_INSERT.format(dto.prometheus_url, dto.display_name)
    await postgresql.execute(query)


async def update_kube_config(postgresql: Postgresql, dto: KubeResponseDTO):
    file_query = ""
    if dto.config:
        file_query = QUERY_KUBE_UPDATE_FILE.format(dto.config)
    query = QUERY_KUBE_UPDATE.format(dto.prometheus_url, dto.display_name, file_query)
    await postgresql.execute(query)


async def select_kube_configs(postgresql: Postgresql,
                              config_required=False, config_slicing=True) -> List[KubeResponseDTO]:
    query = QUERY_KUBE_SELECT.format(QUERY_KUBE_SELECT_HAS_CONFIG if config_required else '')
    rows = await postgresql.fetch_all(query)
    kube_configs = list(map(lambda x: KubeResponseDTO(
        prometheus_url=x[0],
        display_name=x[1],
        config=slice_config(x[2]) if config_slicing else x[2],
        date_created=x[3],
        date_updated=x[4],
    ), rows))
    return kube_configs


def slice_config(config: str | None):
    if config is None:
        return None
    return config[0:100] + '…' if len(config) > 100 else config


async def select_temp(postgresql: Postgresql, name: str) -> TempData:
    query = QUERY_TEMP_SELECT.format(name)
    row = await postgresql.fetch_one(query)
    return TempData(name=name, value=row[0], date_created=row[1], date_updated=row[2])
