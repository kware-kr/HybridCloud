package com.kware.policy.task.collector.service.vo;

import java.util.ArrayList;
import java.util.List;

import com.kware.policy.task.common.constant.StringConstant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Cluster extends ClusterDefault {
	private Integer uid;
	private String nm;
	private String info; //수집한 json데이터
	private String memo;
	private String promUrl;  // 클러스터별 프로메테우스 url=> 2024.07 통합 프로메테우스로 변경
	private String hashVal;
	private Boolean status;
	
	/**/
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private List<ClusterNode> nodes = new ArrayList<ClusterNode>();
	
	public void addClusterNode(ClusterNode _node) {
		this.nodes.add(_node);
	}
	
	public void removeClusterNode(ClusterNode _node) {
		this.nodes.remove(_node);
	}


	public void setStatus(String _status) {
		if (StringConstant.STR_finished.equalsIgnoreCase(_status)) {
			this.status = true;
		} else {
			this.status = false;
		}
	}

	public void setStatus(Boolean _status) {
		this.status = _status;
	}

	@Override
	public String getUniqueKey() {
		return uid.toString();
	}

	@Override
	public void clear() {
		nodes.clear();
	}
}