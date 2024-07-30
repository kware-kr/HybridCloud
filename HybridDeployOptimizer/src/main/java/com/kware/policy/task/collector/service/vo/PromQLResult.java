package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;

import com.kware.policy.task.common.constant.StringConstant;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PromQLResult extends ClusterDefault{
	Integer clUid;
	Integer prqlUid;
	String results;
	Timestamp collectDt;
	
	@Override
	public String getUniqueKey() {
		return clUid + StringConstant.STR_UNDERBAR + prqlUid;
	}
}