from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum, auto
from typing import Optional


class SettingType(StrEnum):
    POSTGRESQL_HOST = "host"
    POSTGRESQL_PORT = "port"
    POSTGRESQL_USER = "user"
    POSTGRESQL_PASSWORD = "password"
    POSTGRESQL_DATABASE = "database"
    PROMETHEUS_API = auto()
    COLLECTING = auto()
    # isinstance(Code.POSTGRESQL_INFO, str)


@dataclass
class Setting:
    type: str
    value: str
    date_created: datetime
    date_updated: Optional[datetime] = None
