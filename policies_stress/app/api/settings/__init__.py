from fastapi import FastAPI

from api.settings.api import router as api_router
from api.settings.svc.postgresql import get_postgresql_info, update_postgresql_info
from api.settings.view import router as view_router
from api.settings.data.table import create_setting_table


def setup(app: FastAPI):
    app.include_router(view_router)
    app.include_router(api_router)

    @app.on_event("startup")
    async def _create_tables():
        await create_setting_table()

        info = await get_postgresql_info()
        if info:
            await update_postgresql_info(info, update_cached_only=True)
