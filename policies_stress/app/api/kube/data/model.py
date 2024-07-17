from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from fastapi_camelcase import CamelModel


class KubeRequestDTO(CamelModel):
    prometheus_url: str
    display_name: str
    date_created: Optional[datetime]
    date_updated: Optional[datetime]


@dataclass
class KubeResponseDTO:
    prometheus_url: str
    display_name: str
    config: Optional[str] = None
    date_created: Optional[datetime] = None
    date_updated: Optional[datetime] = None

    def without_yaml(self):
        return KubeResponseDTO(prometheus_url=self.prometheus_url, display_name=self.display_name,
                               config=None, date_created=self.date_created, date_updated=self.date_updated)


@dataclass
class TempData:
    name: str
    value: Optional[str] = None
    date_created: Optional[datetime] = None
    date_updated: Optional[datetime] = None
