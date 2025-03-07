package com.kware.policy.task.scalor.service.vo;

import com.kware.policy.task.collector.service.vo.PromMetricNode;

import lombok.Data;

@Data
public class NodeScalingInfo {
	public Double  cpu_per;
	public Boolean cpu_isHigh;
	
	public Double  mem_per;
	public Boolean mem_isHigh;
	
	public Double  disk_per;
	public Boolean disk_isHigh;
	
	public Double  gpu_per;
	public Boolean gpu_isHigh;
	
	public Boolean isHigh;
	public int total_node_count;
	public PromMetricNode cur_node;
	
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("NodeScalingInfo { ");
	    sb.append("clusterUid=") .append(cur_node.getClUid()).append(", ");
	    sb.append("node=")       .append(cur_node.getNode()) .append(", ");
	    sb.append("cpu_per=")    .append(cpu_per)            .append(", ");
	    sb.append("cpu_isHigh=") .append(cpu_isHigh)         .append(", ");
	    sb.append("mem_per=")    .append(mem_per)            .append(", ");
	    sb.append("mem_isHigh=") .append(mem_isHigh)         .append(", ");
	    sb.append("disk_per=")   .append(disk_per)           .append(", ");
	    sb.append("disk_isHigh=").append(disk_isHigh)        .append(", ");
	    sb.append("gpu_per=")    .append(gpu_per)            .append(", ");
	    sb.append("gpu_isHigh=") .append(gpu_isHigh)         .append(", ");
	    sb.append("isHigh=")     .append(isHigh)             .append(", ");
	    sb.append("total_node_count=").append(total_node_count);
	    sb.append(" }");
	    return sb.toString();
	}
}



