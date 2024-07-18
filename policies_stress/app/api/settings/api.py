import base64
from copy import deepcopy
from sqlite3 import IntegrityError
from typing import List

from fastapi import Form

from api.settings.data.model import Setting
from api.settings.svc.postgresql import test_postgresql_info, last_postgresql_info, update_postgresql_info
from api.settings.svc.prometheus import get_all_prometheus_apis, new_prometheus_api, delete_prometheus_api
from core.db_postgresql import PostgresqlInfo
from core.error import CustomException
from core.routing import routing

router, _ = routing("settings", frontend=False, auth=True)


@router.get("/apis", description="등록된 프로메테우스 API URL 목록")
async def apis() -> List[Setting]:
    try:
        result = await get_all_prometheus_apis()
    except BaseException as e:
        raise CustomException(override=e)
    return result


@router.post("/apis", description="프로메테우스 API URL 등록")
async def new_api(url: str = Form()):
    try:
        await new_prometheus_api(url)
    except IntegrityError:
        raise CustomException(detail='중복된 URL 입니다.')
    except BaseException as e:
        raise CustomException(override=e)


@router.delete("/apis/{encoded_url}", description="프로메테우스 API URL 삭제")
async def delete_api(encoded_url: str):
    try:
        url = base64.b64decode(encoded_url).decode('utf-8')
        await delete_prometheus_api(url)
    except BaseException as e:
        raise CustomException(override=e)


@router.get("/db", description="postgresql 설정 정보")
async def db_info() -> PostgresqlInfo:
    info = deepcopy(last_postgresql_info)
    info.password = None
    return info


@router.post("/db", description="postgresql 설정 정보 등록/수정")
async def upsert_db_info(info: PostgresqlInfo):
    try:
        await update_postgresql_info(info)
    except BaseException as e:
        raise CustomException(override=e)


@router.post("/db/test", description="postgresql 설정 정보 테스트")
async def test_db_info(info: PostgresqlInfo):
    success = test_postgresql_info(info)
    if not success:
        raise CustomException()
    return True
