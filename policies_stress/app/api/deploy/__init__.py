from fastapi import FastAPI

from api.deploy.api import router as api_router


def setup(app: FastAPI):
    app.include_router(api_router)
