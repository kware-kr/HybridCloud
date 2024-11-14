package com.kware.policy.task.collector.service.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromMetricNodeGPU {

	public PromMetricNodeGPU(String gpuId) {
		this.index = gpuId;
	}

	private String index;
	private Integer temp; //온도
	private String model; //모델명
	private Long capacityMemory; //GPU 메모리 용량 
	private Long availableMemory; //GPU 메모리 가용량
	private Double usage;         //0~1사이의 값 메트릭:DCGM_FI_PROF_GR_ENGINE_ACTIVE

	@Override
	public String toString() {
		return "GPU [index=" + index + ", temp=" + temp + ", model=" + model + ", capacityMemory=" + capacityMemory
				+ ", availableMemory=" + availableMemory + ", usage=" + usage + "]";
	}
	
	public String toJsonString() {
		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"index\":").append(index).append(",");
		//json.append("\"temp\":").append(temp).append(",");
		json.append("\"model\":\"").append(model).append("\",");
		json.append("\"usage\":\"").append(usage).append("\",");
		json.append("\"capacityMemory\":").append(capacityMemory).append(",");
		//json.append("\"availableMemory\":").append(availableMemory).append(",");
		// Remove the last comma if necessary
		if (json.charAt(json.length() - 1) == ',') {
			json.deleteCharAt(json.length() - 1);
		}
		json.append("}");
		return json.toString();
	}
}