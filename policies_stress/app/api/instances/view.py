from fastapi import Request

from core.routing import routing

router, templates = routing("instances", frontend=True, auth=True)


@router.get("/view")
async def instances_view(req: Request):
    return templates.TemplateResponse("pages/instances.html", {"request": req})
