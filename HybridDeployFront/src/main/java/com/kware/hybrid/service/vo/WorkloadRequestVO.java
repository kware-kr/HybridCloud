package com.kware.hybrid.service.vo;

import com.kware.common.openapi.vo.APIPagedRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkloadRequestVO extends APIPagedRequest{
    
    private Long uid; // murq.uid
    private String mlId; // mcw.ml_id
    private String mlNm; // murq.nm
    private String clUid; // mcw.cl_uid
    private String clusterNm; // mc.nm
    private String apiJson; // mcw.info
    private String promPodsJson; // mrup.prom_pods
    private String requestJson; // murq.request_json->'request'
    private String requestDt; // murq.request_dt
    private String notiDt;    // murq.noti_dt
    private String responseJson; // murp.info->'response'
    private String responseCause; // murp.info->'response'
    private String usageInfo; // mcw.usage_info->'response'
    private String completeDt; // murq.request_dt
}