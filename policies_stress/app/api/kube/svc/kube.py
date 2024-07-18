import asyncio
from typing import BinaryIO, List

from kubernetes.config import load_kube_config
from psycopg2.errors import UniqueViolation

from api.kube.data.model import KubeRequestDTO, KubeResponseDTO
from api.kube.data.table import create_kube_config_and_temp_table, insert_kube_config, select_kube_configs, update_kube_config, \
    slice_config
from api.settings.svc.postgresql import last_postgresql_info, get_postgresql_instance
from api.settings.svc.prometheus import get_all_prometheus_apis
from core.db_postgresql import Postgresql
from core.error import CustomException
from core.globals import BusEvents, bus

postgresql = Postgresql()


@bus.on(BusEvents.POSTGRESQL_INFO_UPDATED)
def set_postgresql():
    if last_postgresql_info.is_valid():
        global postgresql
        postgresql = get_postgresql_instance()
        asyncio.create_task(create_kube_config_and_temp_table(postgresql))


async def update_config(config_file: BinaryIO, prometheus_url, display_name) -> KubeResponseDTO:
    if ',' in display_name:
        raise CustomException('표시 명에 ","이 들어갈 수 없습니다.')

    dto = KubeResponseDTO(prometheus_url=prometheus_url, display_name=display_name)

    if config_file:
        try:
            load_kube_config(config_file)
        except BaseException:
            raise CustomException(detail='잘못된 파일입니다.')

        config_file.seek(0)
        dto.config = config_file.read().decode('utf-8')

    await update_kube_config(postgresql, dto)

    dto.config = slice_config(dto.config)
    return dto


async def refresh_and_get_kube_configs() -> List[KubeResponseDTO]:
    if postgresql.db:
        apis = await get_all_prometheus_apis()
        for api in apis:
            url = api.value
            dto = KubeRequestDTO(prometheus_url=url, display_name=url)
            try:
                await insert_kube_config(postgresql, dto)
            except UniqueViolation:
                pass

        configs: List[KubeResponseDTO] = await select_kube_configs(postgresql)
        return configs
