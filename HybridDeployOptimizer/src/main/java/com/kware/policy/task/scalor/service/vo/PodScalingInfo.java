package com.kware.policy.task.scalor.service.vo;

import java.util.HashMap;
import java.util.Map;

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
	
	public Double  cpu_per = 0.0;
	public Double  cpu_val = 0.0;
	public Double  cpu_max_val = 0.0;
	public Boolean cpu_isHigh;
	
	public Double  mem_per = 0.0;
	public Double  mem_val = 0.0;
	public Double  mem_max_val = 0.0;
	public Boolean mem_isHigh;
	
	public Double  disk_per = 0.0;
	public Double  disk_val = 0.0;
	public Double  disk_max_val = 0.0;
	public Boolean disk_isHigh;
	
	public Double  gpu_per = 0.0;
	public Double  gpu_val = 0.0;
	public Double  gpu_max_val = 0.0;
	public Boolean gpu_isHigh;
	
	public int pod_cpu_size  = 0;
	public int pod_mem_size  = 0;
	public int pod_disk_size = 0;
	public int pod_gpu_size  = 0;
	
	public PromMetricPod promMetricPod;
	public PodScalingPolicy ps_policy;
	public String pod_name = null;

	private Map<String, Long> new_limitsMap   = null;
	private Map<String, Long> new_requestsMap = null;
	
	public Map<String, Long> getNewLimitsMap(){
		return new_limitsMap;
	}
	
	public Map<String, Long> getNewRequestsMap(){
		return new_requestsMap;
	}
	
	public void addNewLimitsValue(String key, Long val) {
		if(new_limitsMap == null) {
			new_limitsMap = new HashMap<String, Long>();
		}
		new_limitsMap.put(key, val);
	}
	
	public void addNewRequestsValue(String key, Long val) {
		if(new_requestsMap == null) {
			new_requestsMap = new HashMap<String, Long>();
		}
		new_requestsMap.put(key, val);
	}
	
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
	
	public String getAvearageString() {
		StringBuffer sb = new StringBuffer();

		if (this.cpu_val != 0) {
		    sb.append("CPU:").append(this.cpu_val);
		}

		if (this.mem_val != 0) {
		    if (sb.length() > 0) {
		        sb.append(" ");
		    }
		    sb.append("MEMORY:").append(this.mem_val);
		}

		if (this.disk_val != 0) {
		    if (sb.length() > 0) {
		        sb.append(" ");
		    }
		    sb.append("DISK:").append(this.disk_val);
		}

		if (this.gpu_val != 0) {
		    if (sb.length() > 0) {
		        sb.append(" ");
		    }
		    sb.append("GPU:").append(this.gpu_val);
		}
		
		return sb.toString();
	}
	
	public void clear() {
		if(new_limitsMap != null) {
			new_limitsMap.clear();
		}
		
		if(new_requestsMap != null) {
			new_requestsMap.clear();
		}
	}
	
	public String toStringJson() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("{");
	    
	    if (promMetricPod != null) {
	        sb.append("\"mlId\":")   .append("\"").append(promMetricPod.getMlId()).append("\",");
	        sb.append("\"clulter\":").append("\"").append(promMetricPod.getClUid()).append("\",");
	        sb.append("\"node\":")   .append("\"").append(promMetricPod.getNode()).append("\",");
	        sb.append("\"pod\":")    .append("\"").append(promMetricPod.getPod()).append("\",");
	        sb.append("\"name\":")   .append("\"").append(this.pod_name).append("\",");
	    }
	    
	    sb.append("\"cpu_per\":")   .append(cpu_per).append(",");
	    sb.append("\"cpu_val\":")   .append(cpu_val).append(",");
	    sb.append("\"cpu_max_val\":").append(cpu_max_val).append(",");
	    sb.append("\"cpu_isHigh\":").append(cpu_isHigh).append(",");
	    
	    sb.append("\"mem_per\":")   .append(mem_per).append(",");
	    sb.append("\"mem_val\":")   .append(mem_val).append(",");
	    sb.append("\"mem_max_val\":").append(mem_max_val).append(",");
	    sb.append("\"mem_isHigh\":").append(mem_isHigh).append(",");
	    
	    sb.append("\"disk_per\":")   .append(disk_per).append(",");
	    sb.append("\"disk_val\":")   .append(disk_val).append(",");
	    sb.append("\"disk_max_val\":").append(disk_max_val).append(",");
	    sb.append("\"disk_isHigh\":").append(disk_isHigh).append(",");
	    
	    sb.append("\"gpu_per\":")   .append(gpu_per).append(",");
	    sb.append("\"gpu_val\":")   .append(gpu_val).append(",");
	    sb.append("\"gpu_max_val\":").append(gpu_max_val).append(",");
	    sb.append("\"gpu_isHigh\":").append(gpu_isHigh).append(",");
	    
	    sb.append("\"pod_cpu_size\":") .append(pod_cpu_size).append(",");
	    sb.append("\"pod_mem_size\":") .append(pod_mem_size).append(",");
	    sb.append("\"pod_disk_size\":").append(pod_disk_size).append(",");
	    sb.append("\"pod_gpu_size\":") .append(pod_gpu_size);
	    sb.append("}");
	    return sb.toString();
	}
}
