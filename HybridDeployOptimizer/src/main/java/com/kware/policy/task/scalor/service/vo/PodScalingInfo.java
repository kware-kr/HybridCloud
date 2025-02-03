package com.kware.policy.task.scalor.service.vo;

import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.feature.service.vo.PodScalingPolicy;

public class PodScalingInfo {
	public PodScalingInfo(Double cpu_per, Double mem_per, Double disk_per, Double gpu_per) {
		this.cpu_per  = cpu_per;
		this.mem_per  = mem_per;
		this.disk_per = disk_per;
		this.gpu_per  = gpu_per;
	}
	
	public PodScalingInfo() {
	}
	
	public Double  cpu_per;
	public Double  cpu_val;
	public Boolean cpu_isHigh;
	
	public Double  mem_per;
	public Double  mem_val;
	public Boolean mem_isHigh;
	
	public Double  disk_per;
	public Double  disk_val;
	public Boolean disk_isHigh;
	
	public Double  gpu_per;
	public Double  gpu_val;
	public Boolean gpu_isHigh;
	
	public int pod_cpu_size  = 0;
	public int pod_mem_size  = 0;
	public int pod_disk_size = 0;
	public int pod_gpu_size  = 0;
	
	public PromMetricPod promMetricPod;
	public PodScalingPolicy ps_policy;

	
	public void makeAverage() {
		if(pod_cpu_size > 0) {
			this.cpu_per  = this.cpu_per / pod_cpu_size;
			this.cpu_val  = this.cpu_val / pod_cpu_size;
		}
		if(pod_mem_size > 0) {
			this.mem_per  = this.mem_per / pod_mem_size;
			this.mem_val  = this.mem_val / pod_mem_size;
		}
		if(pod_disk_size > 0) {
			this.disk_per = this.disk_per/ pod_disk_size;
			this.disk_val = this.disk_val/ pod_disk_size;
		}
		if(pod_gpu_size > 0) {
			this.gpu_per  = this.gpu_per / pod_gpu_size;
			this.gpu_val  = this.gpu_val / pod_gpu_size;
		}
	}
}
