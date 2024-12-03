package com.kware.common.db.vo;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class DefaultDaoVO {
	@JsonIgnore
	private Long      regUid;
	@JsonIgnore
	private Timestamp regDt;
	@JsonIgnore
	private Long      updtUid;
	@JsonIgnore
	private Timestamp updtDt;
}	
	