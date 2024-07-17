from typing import List

from api.metrics.data.model import MetricInstance, Collect, Metric, ResourceSet
from api.metrics.data.table import USAGE_METRIC_NAMES
from api.metrics.svc.collect import collect_start, get_latest_instances, get_monitoring_data, collecting, get_resources
from core.error import CustomException
from core.routing import routing

router, _ = routing("metrics", frontend=False, auth=True)


@router.get("/collect", description="메트릭 수집 시작/종료 여부")
async def monitoring_data() -> bool:
    return collecting.start


@router.post("/collect", description="메트릭 수집 시작/종료")
async def monitoring_data(collect: Collect):
    try:
        started = await collect_start(collect.start)
    except BaseException as e:
        raise CustomException(override=e)

    if not started:
        raise CustomException(detail='DB가 설정되어 있지 않거나<br>수집할 프로메테우스 URL이 없습니다.')


@router.get("/instances", description="최근 메트릭 데이터가 존재하는 인스턴스 목록")
async def instances() -> List[MetricInstance]:
    result = await get_latest_instances()
    return result


@router.get("/monitoring", description="해당 인스턴스의 모니터링 데이터")  # 예측을 포함한
async def monitoring_data(node: str, prometheus_url: str,
                          hours: int = 0, minutes: int = 15,
                          metric_names: List[str] = USAGE_METRIC_NAMES) -> List[Metric]:
    result = await get_monitoring_data(node, prometheus_url, hours=hours, minutes=minutes,
                                       metric_names=metric_names)
    return result


@router.get("/resources", description="파드별 CPU 사용량과 최대값")
async def resource_set() -> ResourceSet:
    resources = await get_resources()
    return resources
