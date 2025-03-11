package com.kware.hybrid.service.vo;

import lombok.Data;

@Data
public class ClusterNodeFeatureVO {
	private Long   clUid;
	private Long   nodeUid;          // cluster, node 특성 관련
	private String clusterNm;     //node 특성 관련
	private String nodeInfo;     //node 특성 관련 json
    private String feature;      // 설정 이름 json
    private String autoFeature;
    
    public void setNodeInfo(String nodeInfo) {
    	if(nodeInfo != null)
    		nodeInfo = nodeInfo.replaceAll(":\\s*\"\"", ": null");//공백을 null로 변경
		this.nodeInfo = nodeInfo;
	}
    
	public void setFeature(String feature) {
		if(feature != null)
			feature = feature.replaceAll(":\\s*\"\"", ": null"); //공백을 null로 변경
		this.feature = feature;
	}    
}
