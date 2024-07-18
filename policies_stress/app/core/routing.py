import os
from typing import Optional, Mapping

from fastapi import APIRouter
from fastapi.templating import Jinja2Templates


# from core.depends.user import check_user


def routing(prefix: str = "$", frontend: bool = False, auth: bool = False):
    if prefix:
        prefix = os.path.join("/", prefix)

    if auth:
        router = APIRouter(prefix=prefix, include_in_schema=not frontend)
        # , dependencies = [Depends(check_user)]
    else:
        router = APIRouter(prefix=prefix, include_in_schema=not frontend)

    templates = None

    if frontend:
        templates = FrontTemplate(directory="templates")
    return router, templates


class FrontTemplate(Jinja2Templates):
    def TemplateResponse(
            self,
            name: str,
            context: dict,
            status_code: int = 200,
            headers: Optional[Mapping[str, str]] = None):
        return super().TemplateResponse(name, context, status_code, headers)
