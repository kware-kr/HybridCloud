package com.kware.policy.task.selector.service.vo;

public enum WorkloadResponseStatus {
	
	SUCCESS(          200 , "success"),
	SUCCESS_NO_NODE(  201 , "Suitable node not found"),
	RESCHEDULE(       300 , "Please adjust the resources and retry the request"),
    ERROR(            500 , "Internal server error"),
    INPUT_ERROR(      501 , "Error parsing YAML string")
    ;
    

	private final int code;
	private final String message;

	WorkloadResponseStatus(int code, String message) {
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
		return "ResponseStatus{" + "code='" + code + '\'' + ", message='" + message + '\'' + '}';
	}
}
