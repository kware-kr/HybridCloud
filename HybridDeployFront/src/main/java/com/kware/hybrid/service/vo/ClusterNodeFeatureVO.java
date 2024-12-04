package com.kware.hybrid.service.vo;

import lombok.Data;

@Data
public class ClusterNodeFeatureVO {
	private Long   clUid;
	private Long   nodeUid;          // cluster, node 특성 관련
	private String clusterNm;     //node 특성 관련
	private String nodeInfo;     //node 특성 관련 json
    private String feature;      // 설정 이름 json
    
}
