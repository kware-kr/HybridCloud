from enum import StrEnum, auto

from event_bus import EventBus

bus = EventBus()


class BusEvents(StrEnum):
    POSTGRESQL_INFO_UPDATED = auto()
