package com.kware.common.openapi.vo;


import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIPagedRequest {
    private Integer     pageNumber = 1;        // 요청 페이지 번호
    private Integer     pageSize = 10;          // 페이지 크기
    private String      sortBy;         // 정렬 기준
    private boolean     isSortDesc;  // 정렬 방향 (asc/desc)
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") //전역으로 변경: JacksonDefaultConfig 참고
    private Timestamp   startDt;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp   endDt;

    public APIPagedRequest() {}

    public APIPagedRequest(int pageNumber, int pageSize, String sortBy, boolean isSortDesc) {
        this.pageNumber  = pageNumber;
        this.pageSize    = pageSize;
        this.sortBy      = sortBy;
        this.isSortDesc  = isSortDesc;
    }
    
    public void setDefautPage10() {
    	this.pageNumber = 1;
    	this.pageSize   = 10;
    }
    
    public void setDefautPage20() {
    	this.pageNumber = 1;
    	this.pageSize   = 20;
    }
    
    public void setPageNumber(Integer pageNumber) {
        if(pageNumber != null) this.pageNumber = pageNumber;
    }
    
    public void setPageSize(Integer pageSize) {
        if(pageSize != null) this.pageSize = pageSize;
    }
}