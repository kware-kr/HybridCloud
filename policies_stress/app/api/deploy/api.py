from fastapi.responses import StreamingResponse

from api.deploy.svc.kube import deploy_cpu, check_cpu_over_90_pct, deploy_cpu2
from core.error import CustomException
from core.routing import routing

router, _ = routing("deploy", frontend=False, auth=True)


@router.get("/cpu/1", description="CPU 부하 앱 배포 1단계")
async def deploy_cpu_stress1():
    try:
        iterable = await deploy_cpu()
        return StreamingResponse(iterable)
    except BaseException:
        raise CustomException()


@router.get("/cpu/check", description="CPU 부하 앱 배포 확인")
async def deploy_cpu_stress_check():
    try:
        is_over = await check_cpu_over_90_pct()
        return is_over
    except BaseException:
        raise CustomException()


@router.get("/cpu/2", description="CPU 부하 앱 배포 2단계")
async def deploy_cpu_stress2():
    try:
        iterable = await deploy_cpu2()
        return StreamingResponse(iterable)
    except BaseException:
        raise CustomException()
