package com.kware.common.openapi.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIResponse<T> {
    private int code;    // 응답 코드
    private String message; // 응답 메시지
    private T result;         // 응답 데이터
    
    public APIResponse(int code, String message, T result) {
        this.code    = code;
        this.message = message;
        this.result  = result;
    }
}