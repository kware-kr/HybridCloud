package com.kware.policy.task.feature.service.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.kware.common.db.vo.DefaultDaoVO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CommonFeatureBase extends DefaultDaoVO{
    private String   cfgName;           // 설정 이름
    private String cfgContent;      // 설정 내용
    private String   cfgDesc;           // 설정 설명
    private char     deleteAt = 'N';      // 삭제 여부 ('N' 기본값)
}
