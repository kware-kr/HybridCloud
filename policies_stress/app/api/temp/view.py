from fastapi import Request

from core.routing import routing

router, templates = routing("temp", frontend=True, auth=True)


@router.get("/view")
async def user_parameters_view(req: Request):
    return templates.TemplateResponse("pages/temp.html", {"request": req})
