import traceback

from fastapi import HTTPException


class CustomException(HTTPException):
    def __init__(self, detail: str = '', override: HTTPException = None, trace=False):
        self.status_code = 500

        if override and hasattr(override, 'detail'):
            self.detail = override.detail
        else:
            self.detail = detail

        if trace:
            print(traceback.format_exc())
