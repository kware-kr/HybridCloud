package com.kware.policy.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PromQL extends ClusterDefault{
	long    xmin ; //이 값은 transaction id(system 컬럼)로서 레코드가 변경될때 변경된다.
	Integer clUid;
	Integer prqlUid;
	String grpNm;
	String nm;
	String cont;
	String typeCd;
	String defaultAt;
	String extractPath;
	//String namespace; //namespace를 외부에서 변경하여  promql의 namespace 조건을 변경하기 위함
}