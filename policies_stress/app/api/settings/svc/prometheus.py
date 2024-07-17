from typing import List

from api.instances.data.table import delete_instances
from api.settings.data.model import SettingType, Setting
from api.settings.data.table import select_settings, insert_setting, delete_setting
from api.settings.svc.postgresql import last_postgresql_info, get_postgresql_instance
from core.db_postgresql import Postgresql
from core.globals import BusEvents, bus

postgresql = Postgresql()


@bus.on(BusEvents.POSTGRESQL_INFO_UPDATED)
def set_postgresql():
    if last_postgresql_info.is_valid():
        global postgresql
        postgresql = get_postgresql_instance()


async def get_all_prometheus_apis() -> List[Setting]:
    settings = await select_settings(SettingType.PROMETHEUS_API)
    return settings


async def new_prometheus_api(api_url: str):
    await insert_setting(SettingType.PROMETHEUS_API, api_url)


async def delete_prometheus_api(api_url: str):
    await delete_setting(SettingType.PROMETHEUS_API, api_url)
    await delete_instances(postgresql, api_url)
