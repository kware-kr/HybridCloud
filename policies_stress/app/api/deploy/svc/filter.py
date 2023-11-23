import json
import os
import tempfile
from typing import List

from api.deploy.data.model import ResourceMini
from api.kube.data.model import KubeResponseDTO
from api.metrics.data.model import ResourceMax


def remove_yaml_and_to_json(kube_config: KubeResponseDTO):
    return json.dumps(kube_config.without_yaml().__dict__, default=str)


def parse_json(obj):
    return json.dumps(obj.__dict__, default=str)


def parse_cpu_core(value: str) -> float:
    is_milli = "m" in value.lower()
    value = value.replace("m", "")

    if not is_milli:
        value = float(value) * 1000

    return float(value)


def find_cpu_max(maxes: List[ResourceMax], mini: ResourceMini) -> ResourceMax | None:
    filtered = list(filter(lambda m: m.prom_url == mini.prom_url and m.node == mini.node, maxes))
    if len(filtered) > 0:
        return filtered[0]
    return None


def create_temp_file(text: str):
    fd, file = tempfile.mkstemp()
    with os.fdopen(fd, 'w') as temp:
        temp.write(text)
    return file
