from dataclasses import fields

from api.settings.data.model import SettingType
from api.settings.data.table import select_setting_each, upsert_setting_by_value
from core.db_postgresql import PostgresqlInfo, test_connection, Postgresql
from core.globals import bus, BusEvents

last_postgresql_info = PostgresqlInfo()
postgresql = Postgresql(logging=True)


def test_postgresql_info(info: PostgresqlInfo) -> bool:
    return test_connection(info)


def get_postgresql_instance() -> Postgresql:
    return postgresql


async def get_postgresql_info() -> PostgresqlInfo:
    _dict = await select_setting_each(SettingType.POSTGRESQL_HOST, SettingType.POSTGRESQL_PORT,
                                      SettingType.POSTGRESQL_USER, SettingType.POSTGRESQL_PASSWORD,
                                      SettingType.POSTGRESQL_DATABASE)
    info = PostgresqlInfo()
    for field in fields(PostgresqlInfo):
        setattr(info, field.name, _dict.get(field.name, None))
    return info


async def get_last_postgresql_info() -> PostgresqlInfo:
    return last_postgresql_info


async def update_postgresql_info(info_updated: PostgresqlInfo, update_cached_only=False):
    for field in fields(PostgresqlInfo):
        from_value = getattr(last_postgresql_info, field.name)
        to_value = getattr(info_updated, field.name)

        if to_value != from_value:
            setting_type = SettingType(field.name)
            setattr(last_postgresql_info, field.name, to_value)
            if not update_cached_only:
                await upsert_setting_by_value(setting_type, from_value, to_value)

    postgresql.connect(last_postgresql_info)
    bus.emit(BusEvents.POSTGRESQL_INFO_UPDATED)

    print('Postgresql info updated')
