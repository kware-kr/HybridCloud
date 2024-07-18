from datetime import datetime, timedelta

FULL_FORMAT = "%Y-%m-%d %H:%M:%S.%f"


def dt2str(dt: datetime, format="%Y-%m-%d %H:%M:%S"):
    return dt.strftime(format)


def str2dt(s: str, format="%Y-%m-%d %H:%M:%S"):
    return datetime.strptime(s, format)


def dt2time(dt: datetime):
    return (dt - datetime(1970, 1, 1)).total_seconds()


def time2dt(seconds: float):
    return datetime(1970, 1, 1) + timedelta(seconds=seconds)


def get_time_range(hours: int, minutes: int):
    end_date = datetime.now()
    start_date = end_date - timedelta(hours=hours, minutes=minutes)
    return dt2str(start_date), dt2str(end_date)
