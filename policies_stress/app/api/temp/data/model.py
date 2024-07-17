from fastapi_camelcase import CamelModel


class Param(CamelModel):
    param_name: str
