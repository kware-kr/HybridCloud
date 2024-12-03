package com.kware.hybrid.service.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CommonFeatureVO {
    private String feaName;      // 설정 이름
    private String feaSubName;   // 서브 설정 이름
    private String feaContent;   // 설정 내용 (JSON 데이터)
    private String feaDesc;      // 설정 설명
    private String deleteAt;     // 삭제 여부
    private Long regUid;         // 등록 사용자 ID
    private Timestamp regDt;     // 등록 일시
    private Long updtUid;        // 수정 사용자 ID
    private Timestamp updtDt;    // 수정 일시
}
