package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/* json 샘플
"result":{ ...
		"resources": [
            {
                "id": 479,
                "name": "keti-ml-workload-workload-009zc89s",
                "kind": "Workflow",
                "createdAt": "2024-09-20 16:28:42.0",
                "updatedAt": "2024-09-20 16:28:42.0",
                "uid": null,
                "status": null,
                "resourceId": null,
                "yaml": "...",
                "pods": [
                    {
                        "uid": "f58585d5-8515-40cf-b809-6ac4a6a7a82d",
                        "name": "keti-ml-workload-workload-009zc89s-keti-ml-workload-preprocess-3984749905",
                        "namespace": "keti-crd",
                        "labels": {
                            "ml.workload": "preprocess",
                            "pipelines.kubeflow.org/enable_caching": "true",
                            "pipelines.kubeflow.org/kfp_sdk_version": "1.8.22",
                            "pipelines.kubeflow.org/pipeline-sdk-type": "kfp",
                            "workflows.argoproj.io/completed": "false",
                            "workflows.argoproj.io/workflow": "keti-ml-workload-workload-009zc89s"
                        },
                        "annotations": {
                            "cni.projectcalico.org/containerID": "19684172dd17e6fdc4d49a7e575f1cd9c1c13af9dff171f1af3371c9de63f520",
                            "cni.projectcalico.org/podIP": "192.168.55.110/32",
                            "cni.projectcalico.org/podIPs": "192.168.55.110/32",
                            "kubectl.kubernetes.io/default-container": "main",
                            "workflows.argoproj.io/node-id": "keti-ml-workload-workload-009zc89s-3984749905",
                            "workflows.argoproj.io/node-name": "keti-ml-workload-workload-009zc89s.keti-ml-workload-preprocess"
                        },
                        "kind": "Pod",
                        "createdAt": "2024-09-20 16:28:42",
                        "ip": "192.168.55.110",
                        "qosClass": "Burstable",
                        "images": [
                            "quay.io/argoproj/argoexec:latest",
                            "chromatices/resource_stress:1.7"
                        ],
                        "node": "gpu-01",
                        "restart": 0,
                        "status": "Running",
                        "ownerUid": "2154546c-7031-48c5-a94e-bff3af3f76b0",
                        "ownerKind": "Workflow",
                        "ownerName": "keti-ml-workload-workload-009zc89s",
                        "condition": [
                            {
                                "lastTransitionTime": "2024-09-20T07:28:44Z",
                                "status": "True",
                                "type": "Initialized",
                                "additionalProperties": {}
                            },
...
                        ],
                        "pvcList": []
                    }
                ],
                "totalPodCount": 0,
                "runningPodCount": 0
            }
        ]
        ...
}
*/

/*
 * 스트라토 워크로드 API의 상세조회 결과 resource를 관리
 * mlId는 
 */
@Getter
@Setter
@ToString
public class ClusterWorkloadResource extends ClusterDefault {
	private Integer clUid; //배포된 클러스터 아이디
	private Integer id;    //기본 ID
	private String nm;     //워크로드 이름
	private String kind;   //워크로드의 종류
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(type = "string", pattern = "yyyy-MM-dd HH:mm:ss", example = "2024-10-29 15:30:00")
	private Timestamp   createdAt;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(type = "string", pattern = "yyyy-MM-dd HH:mm:ss", example = "2024-10-29 15:30:00")
	private Timestamp   updatedAt;
	
	private String uid;
	//private APIConstant.WorkloadStatus status;
	private String status;
	private String resourceId;
	
	private int totalPodCount;
	private int runningPodCount;

	
	@Override
	public void clear() {
		podMap.clear();
		podMap = null;
	}
	
	@JsonIgnore
	@Override
	public String getUniqueKey() {
		//uid를 프로메테우스에서는 가져올 수 없고 name정보만 가져올 수 있다.
		return nm;
	}
		
	// @Setter(AccessLevel.NONE)
	//@Getter(AccessLevel.NONE)
	//pod uid를 key로 함
	@JsonIgnore
	private Map<String, ClusterWorkloadPod> podMap = new HashMap<String, ClusterWorkloadPod>();

	public Map<String, ClusterWorkloadPod> getPods(){
		return this.podMap;
	}
	
	public ClusterWorkloadPod getPod(String _podUid) {
		return podMap.get(_podUid);
	}

	public Boolean containsPod(String _podUid) {
		return podMap.containsKey(_podUid);
	}

	/**
	 * ClusterWorkload에 포함되어 있는 pod를 관리한다.
	 * @param _podUid
	 * @param _pod
	 */
	public void addPod(ClusterWorkloadPod _pod) {
		podMap.put(_pod.getUid(), _pod);
	}
	
	/*
	public void setStatusString(String status) {
		if (status != null && !status.isEmpty()) {
            try {
                this.status =  APIConstant.WorkloadStatus.valueOf(status); // String을 Enum으로 변환
            } catch (IllegalArgumentException e) {
            	this.status = null;
            }
        }
	}	
	
	@JsonIgnore
	public String getStatuString() {
        if (status != null) {
            return status.name(); // Enum의 name() 메서드를 사용해 String으로 변환
        }
        return null;
    }
	*/
}