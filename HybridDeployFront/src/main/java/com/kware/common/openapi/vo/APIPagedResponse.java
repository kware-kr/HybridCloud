package com.kware.common.openapi.vo;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIPagedResponse<T> extends APIResponse<List<T>> {
    private int  pageNumber;      // 현재 페이지 번호
    private int  pageSize;        // 페이지 크기
    private int  pageElements;    // 현재 페이지 엘리먼트 크기
    private long totalElements;   // 전체 데이터 수
    private int  totalPages;      // 전체 페이지 수

    /**
     * 
     * @param code
     * @param message
     * @param data
     * @param pageNumber
     * @param pageSize
     * @param totalElements
     * @param totalPages
     */
    public APIPagedResponse(int code, String message, List<T> data, int pageNumber, int pageSize, long totalElements, int totalPages) {
        super(code, message, data);
        this.pageNumber    = pageNumber;
        this.pageSize      = pageSize;
        this.totalElements = totalElements;
        this.totalPages    = totalPages;
        this.pageElements  = data.size();
    }
    
    /**
     * default value is totalElements = -1, totalPages = -1 
     * @param code
     * @param message
     * @param data
     * @param pageNumber
     * @param pageSize
     */
    public APIPagedResponse(int code, String message, List<T> data, int pageNumber, int pageSize) {
    	 this(code, message, data, pageNumber, pageSize, -1, -1);
    }
    
    /**
     * default value is pageSize = 10, totalElements = -1, totalPages = -1
     * @param code
     * @param message
     * @param data
     * @param pageNumber
     */
    public APIPagedResponse(int code, String message, List<T> data, int pageNumber) {
    	this(code, message, data, pageNumber, 10);
    }
    
    /**
     * default value is pageNumber = 1, pageSize = 10, totalElements = -1, totalPages = -1
     * @param code
     * @param message
     * @param data
     * @param pageNumber
     */
    public APIPagedResponse(int code, String message, List<T> data) {
    	this(code, message, data, 1);
    }
}