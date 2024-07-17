import json
from dataclasses import dataclass
from typing import Literal, Optional

ResultType = Literal['str', 'json', 'int', 'float', 'list']


@dataclass
class Result:
    success: bool
    type: ResultType = 'str'
    size: int = 0

    def to_json(self):
        return json.dumps(self.__dict__, default=str)

    @staticmethod
    def success(_type: ResultType = 'str', size=0):
        return Result(success=True, type=_type, size=size)

    @staticmethod
    def fail(size=0):
        return Result(success=False, type='str', size=size)


@dataclass
class ResourceQuota:
    prom_url: str
    node: str
    name: str
    requests: float
    limits: float
    remain: float


@dataclass
class ResourceMini:
    prom_url: str
    node: str
    value: float


@dataclass
class InstanceMini:
    prom_url: str
    node: str
    sort_order: int


@dataclass
class HikaruMini:
    kind: str
    name: str
    namespace: Optional[str] = None
