package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
/* json 샘플
"result": { ...
"resources": [
            {
                ...
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
                ]
                ...
            }
        ]
        ...
}
*/
 

@Getter
@Setter
@ToString
public class ClusterWorkloadPod extends ClusterDefault {
	private String uid;  // pod uid
	private String kind; // kind 이름
	private String pod;  // pod이름 name
	private String node; // 노드이름
	private String mlId; // ml이름
	private Integer clUid;
	
	//private long   createdAt;
	//private long   updatedAt;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   createdAt;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp   updatedAt;
	
	private String namespace;
	private String ownerUid; // 이것 프로메테우스에서 찾을 수 없네.
	private String ownerName;
	private Integer restart;
	private String ownerKind;
	
	private String status;
	
	private boolean isCompleted;

	@JsonIgnore
	@Override
	public String getUniqueKey() {
		return uid;
	}
}