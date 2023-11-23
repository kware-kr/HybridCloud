from typing import List

from api.instances.data.model import Instance, InstanceDto
from api.instances.svc.crud import refresh_and_get_instances, modi_instance, modi_instances_sort_order
from core.error import CustomException
from core.routing import routing

router, _ = routing("instances", frontend=False, auth=True)


@router.get("/", description="인스턴스 목록")
async def instances() -> List[InstanceDto]:
    result = await refresh_and_get_instances()
    return result


# @router.post("/", description="새 인스턴스")
# async def new_inst(instance: Instance):
#     try:
#         await new_instance(instance)
#     except IntegrityError:
#         raise CustomException('중복된 인스턴스 이름입니다.')
#     except BaseException as e:
#         raise CustomException(override=e)


@router.put("/", description="인스턴스 수정")
async def modi_inst(instance: Instance):
    try:
        await modi_instance(instance)
    except BaseException as e:
        raise CustomException(override=e)


@router.put("/order", description="인스턴스들의 우선순위 수정")
async def modi_inst(_instances: List[Instance]):
    try:
        await modi_instances_sort_order(_instances)
    except BaseException as e:
        raise CustomException(override=e)
