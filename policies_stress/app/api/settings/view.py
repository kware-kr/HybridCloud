from fastapi import Request

from core.routing import routing

router, templates = routing("settings", frontend=True, auth=True)


@router.get("/db/view")
async def postgresql(req: Request):
    return templates.TemplateResponse("pages/postgresql.html", {"request": req})


@router.get("/apis/view")
async def prometheus_api(req: Request):
    return templates.TemplateResponse("pages/prometheus.html", {"request": req})
