from typing import List

from fastapi import UploadFile, File, Form
from fastapi.responses import Response

from api.kube.data.model import KubeResponseDTO, KubeRequestDTO
from api.kube.svc.kube import update_config, refresh_and_get_kube_configs
from core.routing import routing

router, _ = routing("kube", frontend=False, auth=True)


@router.post("/", description="쿠버네티스 config 정보 수정")
async def upload_config_file(prometheus_url: str = Form(alias="prometheusUrl"),
                             display_name: str = Form(alias="displayName"),
                             config: UploadFile = File()) -> KubeResponseDTO:
    file = None
    if config.filename:
        file = config.file

    result = await update_config(file, prometheus_url, display_name)
    return result


@router.get("/", description="쿠버네티스 config 정보 리스트")
async def get_configs() -> List[KubeResponseDTO]:
    configs = await refresh_and_get_kube_configs()
    return configs
