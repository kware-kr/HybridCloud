from dataclasses import dataclass
from datetime import datetime
from enum import StrEnum, auto
from typing import Optional, List


class MetricName(StrEnum):
    CPU_USAGE = auto()
    DSK_USAGE = auto()
    MEM_USAGE = auto()
    GPU_USAGE = auto()
    DSK_IO = auto()


@dataclass
class RawMetric:
    metric_name: str
    instance: str
    value: str
    prometheus_url: str
    date_created: Optional[datetime] = None


@dataclass
class MetricInstance:
    instance: str
    prometheus_url: str


@dataclass
class Metric:
    cpu_usage: Optional[float] = 0
    dsk_usage: Optional[float] = 0
    mem_usage: Optional[float] = 0
    gpu_usage: Optional[float] = 0
    dsk_io: Optional[float] = 0
    date_created: Optional[datetime] = None
    is_predict = False


@dataclass
class Collect:
    start: bool


@dataclass
class Resource:
    prom_url: str
    data_type: str
    namespace: str
    node: str
    pod: str
    value: float


@dataclass
class ResourceMax:
    prom_url: str
    data_type: str
    node: str
    value: str


@dataclass
class ResourceSet:
    value: List[Resource]
    max: List[ResourceMax]
