from fastapi import FastAPI

from api import metrics, settings, instances, temp, deploy, kube


def setup(app: FastAPI):
    metrics.setup(app)
    settings.setup(app)
    instances.setup(app)
    temp.setup(app)
    deploy.setup(app)
    kube.setup(app)
