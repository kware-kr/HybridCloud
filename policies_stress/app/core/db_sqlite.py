import os
from sqlite3 import Row
from typing import Iterable

import aiosqlite


class Sqlite:
    def __init__(self, logging=False):
        db_dir = os.path.expanduser('data')
        if not os.path.exists(db_dir):
            os.mkdir(db_dir)

        self.db_path = os.path.join(db_dir, 'data')
        open(self.db_path, 'a').close()

        self.logging = logging

    async def execute(self, query: str, *args: str) -> bool:
        # try:
        if self.logging:
            print(query.strip() + ';')
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute(query, args)
            await db.commit()
            changed = db.total_changes > 0
        return changed
        # except:
        #     tb = traceback.format_exc()
        #     print(tb)

    async def fetch_all(self, query: str, *args: str) -> Iterable[Row]:
        if self.logging:
            print(query.strip() + ';')
        async with aiosqlite.connect(self.db_path) as db:
            cursor = await db.execute(query, args)
            rows = await cursor.fetchall()
        return rows

    async def fetch_one(self, query: str, *args: str) -> Row:
        if self.logging:
            print(query.strip() + ';')
        async with aiosqlite.connect(self.db_path) as db:
            cursor = await db.execute(query, args)
            row = await cursor.fetchone()
        return row
