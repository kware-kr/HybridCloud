from typing import List, Dict

from api.settings.data.model import Setting, SettingType
from core.db_sqlite import Sqlite

QUERY_SETTING_TABLE = """
CREATE TABLE IF NOT EXISTS setting
(
    TYPE         text NOT NULL,
    VALUE        text NOT NULL,
    date_created datetime DEFAULT (datetime('now','localtime')),
    date_updated datetime,
    PRIMARY KEY (TYPE, VALUE)
)"""

QUERY_SETTING_INSERT = "INSERT INTO setting (type, value) VALUES ('{0}', '{1}')"

QUERY_SETTING_SELECT = "SELECT value, date_created, date_updated FROM setting WHERE type = '{0}'"

QUERY_SETTING_SELECT_BY_VALUE = "SELECT value, date_created, date_updated " \
                                "FROM setting WHERE type = '{0}' AND value = '{1}'"

QUERY_SETTING_UPDATE = "UPDATE setting SET value = '{1}' WHERE type = '{0}'"

QUERY_SETTING_UPDATE_BY_VALUE = "UPDATE setting SET value = '{2}' WHERE type = '{0}' AND value = '{1}'"

QUERY_SETTING_DELETE = "DELETE FROM setting WHERE type = '{0}' AND value = '{1}'"

QUERY_SETTING_SELECT_EACH = "(SELECT value FROM setting WHERE type = '{0}') AS {1}"

sqlite = Sqlite(logging=True)


async def create_setting_table():
    await sqlite.execute(QUERY_SETTING_TABLE)


async def select_setting_each(*setting_types: SettingType) -> Dict[str, str]:
    query = "SELECT "
    is_first = True
    _dict = {}
    column_names = []

    for setting_type in setting_types:
        column_names.append(setting_type.value)
        column = QUERY_SETTING_SELECT_EACH.format(setting_type.name, setting_type.name)
        if is_first:
            is_first = False
            query += column
        else:
            query += "\n     , {0}".format(column)

    settings = await sqlite.fetch_all(query)
    for i in range(len(settings[0])):
        _dict[column_names[i]] = settings[0][i]

    return _dict


async def insert_setting(setting_type: SettingType, value: str):
    query = QUERY_SETTING_INSERT.format(setting_type.name, value)
    await sqlite.execute(query)


async def select_settings(setting_type: SettingType) -> List[Setting]:
    query = QUERY_SETTING_SELECT.format(setting_type.name)
    rows = await sqlite.fetch_all(query)
    return list(map(lambda x: Setting(
        type=setting_type,
        value=x[0],
        date_created=x[1],
        date_updated=x[2]
    ), rows))


async def update_setting(setting_type: SettingType, value: str):
    query = QUERY_SETTING_UPDATE.format(setting_type.name, value)
    await sqlite.execute(query)


async def update_setting_by_value(setting_type: SettingType, from_value: str, to_value: str):
    query = QUERY_SETTING_UPDATE_BY_VALUE.format(setting_type.name, to_value, from_value)
    await sqlite.execute(query)


async def upsert_setting(setting_type: SettingType, value: str):
    rows = await sqlite.fetch_all(QUERY_SETTING_SELECT.format(setting_type.name))
    if len(rows) > 0:
        await update_setting(setting_type, value)
    else:
        await insert_setting(setting_type, value)


async def upsert_setting_by_value(setting_type: SettingType, from_value: str, to_value: str):
    rows = await sqlite.fetch_all(QUERY_SETTING_SELECT_BY_VALUE.format(setting_type.name, from_value))
    if len(rows) > 0:
        await update_setting_by_value(setting_type, from_value, to_value)
    else:
        await insert_setting(setting_type, to_value)


async def delete_setting(setting_type: SettingType, value: str):
    query = QUERY_SETTING_DELETE.format(setting_type.name, value)
    await sqlite.execute(query)
