package com.kware.policy.task.feature.service.vo;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class ClusterNodeFeature {
    private Integer noUid;        // 노드 UID
    private Short genLevel;       // 일반 성능 레벨
    private Short gpuLevel;       // GPU 성능 레벨
    private Short secLevel;       // 보안 레벨
    private String cloudType;     // 클라우드 구분 (PRI, PUB, ONP)
    private JsonNode etc;         // 추가 설정
    
    public boolean isEqual(ClusterNodeFeature other) {
        if (this == other) return true;
        if (other == null) return false;

        return 
            this.noUid.equals(other.noUid) &&
            this.genLevel.equals(other.genLevel) &&
            this.gpuLevel.equals(other.gpuLevel) &&
            this.secLevel.equals(other.secLevel) &&
            this.cloudType.equals(other.cloudType) &&
            (this.etc == null ? other.etc == null : this.etc.equals(other.etc)
        );
    }

}
