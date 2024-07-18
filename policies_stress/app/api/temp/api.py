from core.routing import routing

router, _ = routing("temp", frontend=False, auth=True)

# @router.get("/", description="인스턴스 목록")
# async def instances() -> List[InstanceDto]:
#     result = await refresh_and_get_instances()
#     return result
