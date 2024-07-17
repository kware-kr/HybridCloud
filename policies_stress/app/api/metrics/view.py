from fastapi import Request

from core.routing import routing

router, templates = routing("metrics", frontend=True, auth=True)


@router.get("/monitoring/view")
async def monitoring(req: Request):
    return templates.TemplateResponse("pages/monitoring.html", {"request": req})
