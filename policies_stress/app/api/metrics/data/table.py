from sqlite3 import Row
from typing import List, Iterable

from api.metrics.data.model import RawMetric, MetricInstance
from core.db_postgresql import Postgresql
from util.date import get_time_range

QUERY_METRIC_TABLE = """
CREATE TABLE IF NOT EXISTS metric
(
    metric_name    text,
    INSTANCE       text,
    VALUE          text,
    prometheus_url text,
    description    text,
    date_created   TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
)"""

QUERY_METRIC_SELECT = """
SELECT date_trunc('second', date_created) {0}
  FROM metric
 WHERE INSTANCE = '{1}'
   AND prometheus_url = '{2}'
   AND date_created BETWEEN '{3}' AND '{4}'
 GROUP BY date_trunc('second', date_created)
 ORDER BY date_trunc('second', date_created)"""

QUERY_METRIC_SELECT_ONE = """
     , MAX(CASE WHEN (metric_name = '{0}') THEN value END) AS {0}
"""

QUERY_METRIC_INSERT = """
INSERT INTO metric (metric_name, instance, value, prometheus_url, date_created)
VALUES {0}
"""

QUERY_METRIC_INSERT_ROW = "('{0}', '{1}', '{2}', '{3}', '{4}')"

QUERY_METRIC_SELECT_INSTANCE = """
SELECT instance
     , prometheus_url
  FROM metric {0} 
 GROUP BY INSTANCE, prometheus_url
 ORDER BY INSTANCE, prometheus_url
"""

QUERY_WHERE_CLAUSE = "WHERE date_created BETWEEN '{0}' AND '{1}'"

USAGE_METRIC_NAMES = [
    'cpu_usage',
    'dsk_usage',
    'mem_usage',
    'gpu_usage',
    'dsk_io'
]

MAX_METRIC_NAMES = [
    'cpu_max',
    'dsk_max',
    'mem_max',
]


async def create_metric_table(postgresql: Postgresql):
    await postgresql.execute(QUERY_METRIC_TABLE)


async def insert_metrics(postgresql: Postgresql, metrics: List[RawMetric], now):
    if len(metrics) == 0:
        return

    rows = []
    for metric in metrics:
        query = QUERY_METRIC_INSERT_ROW.format(metric.metric_name, metric.instance,
                                               metric.value, metric.prometheus_url, now)
        rows.append(query)
    query = QUERY_METRIC_INSERT.format('\n, '.join(rows))
    await postgresql.execute(query)


async def select_metrics(postgresql: Postgresql,
                         instance: str, prometheus_url: str,
                         hours=0, minutes=15,
                         metric_names: List[str] = USAGE_METRIC_NAMES) -> Iterable[Row]:
    """
    hours + minutes 전 까지의 메트릭 데이터(사용률)

    Args:
        postgresql: db instance
        instance: 메트릭 수집 대상 노드
        prometheus_url: 메트릭 수집 대상 프로메테우스 URL
        hours: 시
        minutes: 분
        metric_names: 조회할 메트릭

    Returns: 메트릭 데이터 sqlite3.Row 배열
    """
    cols = ""
    for metric_name in metric_names:
        cols += QUERY_METRIC_SELECT_ONE.format(metric_name).rstrip()

    start, end = get_time_range(hours, minutes)
    query = QUERY_METRIC_SELECT.format(cols, instance, prometheus_url, start, end)

    rows: Iterable[Row] = await postgresql.fetch_all(query)
    return rows


async def select_metric_instances(postgresql: Postgresql, hours=0, minutes=15, with_date_range=True) \
        -> List[MetricInstance]:
    start, end = get_time_range(hours, minutes)
    where = ''
    if with_date_range:
        where = QUERY_WHERE_CLAUSE.format(start, end)

    query = QUERY_METRIC_SELECT_INSTANCE.format(where)
    instances = await postgresql.fetch_all(query)

    return list(map(lambda inst: MetricInstance(instance=inst[0], prometheus_url=inst[1]), instances))
