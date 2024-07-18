from fastapi import Request

from core.routing import routing

router, templates = routing("kube", frontend=True, auth=True)


@router.get("/view")
async def kube(req: Request):
    return templates.TemplateResponse("pages/kube.html", {"request": req})
