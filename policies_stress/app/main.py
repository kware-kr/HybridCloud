from fastapi import FastAPI, Request
from fastapi.responses import Response
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from starlette.types import Scope

import api


class StaticFilesWrapper(StaticFiles):
    async def get_response(self, path: str, scope: Scope) -> Response:
        response: Response = await super().get_response(path, scope)
        response.headers.append("Cache-Control", "max-age=2592000")  # 30d
        return response


app = FastAPI()
app.mount("/static", StaticFilesWrapper(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

api.setup(app)


@app.get("/")
async def index(req: Request):
    return templates.TemplateResponse("pages/index.html", {"request": req})
