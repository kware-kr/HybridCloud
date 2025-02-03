package com.kware.policy.task.scalor.service.vo;

import com.kware.policy.task.collector.service.vo.PromMetricNode;

import lombok.Data;

@Data
public class NodeScalingInfo {
	public Double cpu_per;
	public Double mem_per;
	public Double disk_per;
	public Double gpu_per;
	public Boolean isHigh;
	public PromMetricNode cur_node;
}
