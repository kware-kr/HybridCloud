package com.kware.policy.task.common.service.vo;


import java.time.LocalDateTime;

import com.kware.common.openapi.vo.APIPagedRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommonEvent extends APIPagedRequest{
    private Long id;
    private String name;
    private String eventType;
    private String description;
    private LocalDateTime regDt; // 등록 일시
    
    public CommonEvent(String name, String eventType, String description) {
        this.name = name;
        this.eventType = eventType;
        this.description = description;
    }

	public CommonEvent() {
	}
}
