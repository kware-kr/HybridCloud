package com.kware.hybrid.service.vo;

import lombok.Data;

@Data
public class DashboardVO{
    
    private Long clUid; 
    private String cluster; //클러스터 이름
    private String node; //클러스터 이름
    private String nodes;    //노드 json 리스트 
    private String pods;    //파드 json 리스트
}