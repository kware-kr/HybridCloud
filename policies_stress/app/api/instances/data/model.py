from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from fastapi_camelcase import CamelModel


class Instance(CamelModel):
    instance_name: str
    prometheus_url: str
    enabled: str = None
    sort_order: int = None
    display_name: str = None


@dataclass
class InstanceDto:
    instance_name: str
    prometheus_url: str
    enabled: str = None
    sort_order: int = None
    display_name: str = None
    date_created: Optional[datetime] = None
