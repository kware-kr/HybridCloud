from fastapi import FastAPI

from api.instances.api import router as api_router
from api.instances.view import router as view_router


def setup(app: FastAPI):
    app.include_router(view_router)
    app.include_router(api_router)
