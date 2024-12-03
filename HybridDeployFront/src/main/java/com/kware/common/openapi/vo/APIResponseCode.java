package com.kware.common.openapi.vo;

public enum APIResponseCode {
	//workload node 요청 message
	SUCCESS                (200 , "success"),
	SUCCESS_NO_NODE        (201 , "Suitable node not found"),
	RESCHEDULE             (300 , "Please adjust the resources and retry the request"),
	
	//API 일반 응답 메시지 
    UNAUTHORIZED_ERROR     (401 , "Unauthorized error"),
    NOT_FOUND_ERROR        (404 , "Not found error"),
    NOT_ALLOWED_METHOD     (405 , "Not allowed method error"),
    
    INTERNAL_SYSTEM_ERROR  (500 , "Internal system error"),
    INPUT_ERROR            (501 , "Error parsing JSON or YAML string"),
    INVALID_PARAMETER_ERROR(502 , "Invalid parameter error"),
    UNKNOWN_ERROR          (520 , "Unknown error"),
    LICENSE_ERROR          (521 , "License error"),
    TOKEN_EXPIRED          (522 , "Token expired"),
    INTERNAL_DATABASE_ERROR(601 , "Internal database error");
    ;
    

	private final int code;
	private final String message;

	APIResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "APIResponseCode{" + "code='" + code + '\'' + ", message='" + message + '\'' + '}';
	}
}
