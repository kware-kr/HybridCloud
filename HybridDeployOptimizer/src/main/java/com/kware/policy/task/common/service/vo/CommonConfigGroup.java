package com.kware.policy.task.common.service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.kware.common.db.vo.DefaultDaoVO;
import com.kware.common.util.JSONUtil;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;

@Getter
@Setter
public class CommonConfigGroup extends DefaultDaoVO{

    private String feaName;      // 설정 이름
    @JsonIgnore
    private String feaContent; // 설정 내용 (JSONB)
    private String feaDesc;      // 설정 설명

	/*   public static enum ConfigName{
		workload_feature,
		security_level,
		gpu_level,
		priorityClass,
		cloud_type,
		workload_deployment_stage,
		workload_type,
		node_performamce_level;
		
		public static ConfigName getConfigName(String value) {
	        try {
	        	ConfigName a = ConfigName.valueOf(value);
	            return a;
	        } catch (IllegalArgumentException e) {
	            return null;
	        }
	    }
	}*/
    
    @JsonProperty("feaContent")
    public JsonNode getContent() throws Exception {
    	return JSONUtil.fromJson(feaContent, JsonNode.class);
    }
}
