from dataclasses import dataclass
from sqlite3 import Row
from typing import Iterable, Optional

import psycopg2
from psycopg2.errors import InFailedSqlTransaction

from core.error import CustomException


@dataclass
class PostgresqlInfo:
    host: Optional[str] = None
    port: Optional[str] = None
    user: Optional[str] = None
    password: Optional[str] = None
    database: Optional[str] = None

    def is_valid(self):
        return not (not self.host and
                    not self.port and
                    not self.user and
                    not self.password and
                    not self.database)


def test_connection(info: PostgresqlInfo) -> bool:
    try:
        db = psycopg2.connect(host=info.host, port=info.port,
                              user=info.user, password=info.password,
                              database=info.database)
        connected = db.closed == 0
        db.close()
    except psycopg2.Error:
        return False
    return connected


class Postgresql:
    def __init__(self, logging=False):
        self.db = None
        self.info: PostgresqlInfo = None
        self.logging = logging
        self.cursor = None

    def connect(self, info: PostgresqlInfo):
        self.info = info
        self.db = psycopg2.connect(host=info.host, port=info.port,
                                   user=info.user, password=info.password,
                                   database=info.database)
        self.cursor = self.db.cursor()

    def get_cursor(self):
        if self.db.closed != 0:
            self.connect(self.info)
        return self.cursor

    def _execute(self, query, args):
        try:
            self.get_cursor().execute(query, args)
        except InFailedSqlTransaction:
            self.db.rollback()
            self.get_cursor().execute(query, args)

    async def execute(self, query: str, *args: str) -> bool:
        if self.logging:
            print(query.strip() + ';')
        self._execute(query, args)
        self.db.commit()
        return self.get_cursor().rowcount

    async def fetch_all(self, query: str, *args: str) -> Iterable[Row]:
        if self.logging:
            print(query.strip() + ';')
        self._execute(query, args)
        rows = self.get_cursor().fetchall()
        return rows

    async def fetch_one(self, query: str, *args: str) -> Row:
        if self.logging:
            print(query.strip() + ';')
        self._execute(query, args)
        row = self.get_cursor().fetchone()
        return row
