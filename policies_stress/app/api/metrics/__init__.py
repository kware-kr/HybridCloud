from fastapi import FastAPI
from fastapi_utils.tasks import repeat_every

from api.metrics.api import router as api_router
from api.metrics.svc.collect import collect_with_api, is_collecting, collect_start, get_latest_instances
from api.metrics.view import router as view_router


def setup(app: FastAPI):
    app.include_router(view_router)
    app.include_router(api_router)

    @app.on_event("startup")
    @repeat_every(seconds=15)
    async def every_15s():
        await collect_with_api()
