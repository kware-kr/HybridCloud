package com.kware.policy.service.vo;

import java.sql.Timestamp;

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
}