package com.kware.policy.task.common.constant;

public class StringConstant {

	public final static String STR_mlId          = "mlId";
	public final static String STR_clusterIdx    = "clusterIdx";
	public final static String STR_uid           = "uid";
	public final static String STR_name          = "name";
	public final static String STR_status        = "status";
	public final static String STR_labels        = "labels";
	public final static String STR_description   = "description";
	public final static String STR_prometheusUrl = "prometheusUrl";
	public final static String STR_finished      = "finished";
	public final static String STR_healthy       = "healthy";
	public final static String STR_true          = "true";
	public final static String STR_false         = "false";
	public final static String STR_userId        = "userId";
	public final static String STR_id            = "id";
	public final static String STR_driverVersion = "driverVersion";
	public final static String STR_cudaVersion   = "cudaVersion";
	public final static String STR_createAt      = "createAt";
	public final static String STR_version       = "version";
	public final static String STR_createdAt     = "createdAt";
	public final static String STR_updatedAt     = "updatedAt";
	public final static String STR_role          = "role";
	
	public final static String STR_pod           = "pod";
	public final static String STR_pods          = "pods";
	public final static String STR_resources     = "resources";
	public final static String STR_kind          = "kind";
	public final static String STR_node          = "node";
	public final static String STR_namespace     = "namespace";
	public final static String STR_ownerUid      = "ownerUid";
	public final static String STR_ownerName     = "ownerName";
	public final static String STR_ownerKind     = "ownerKind";
	public final static String STR_restart       = "restart";
	public final static String STR_totalPodCount   = "totalPodCount";
	public final static String STR_runningPodCount = "runningPodCount";
	
	public final static String STR_Y             = "Y";
	public final static String STR_N             = "N";
	public final static String STR_0             = "0";
	public final static String STR_1             = "1";
	
	public final static String STR_Authorization    = "Authorization";
	public final static String STR_Content_Type     = "Content-Type";
	public final static String STR_application_json = "application/json";
	
	public final static String JSON_EMPTY        = "{}";
	public final static String STR_UNDERBAR      = "_";
	public final static String STR_COMMA         = ",";
	
	
	public static enum RequestStatus{
		request,
	    complete
	};
	
	public static enum PodStatusPhase{
		PENDING,
	    RUNNING,
	    SUCCEEDED,
	    FAILED,
	    UNKNOWN
	};
	
	public static enum PriorityClass{
		criticalPriority, highPriority, mediumPriority, lowPriority, veryLowPriority
	};
	
	public static enum PreemptionPolicy{
		PreemptLowerPriority, Never
	};
	
	
	/*
	public static enum KubeKind {
	    POD("Pod"),
	    SERVICE("Service"),
	    DEPLOYMENT("Deployment"),
	    REPLICASET("ReplicaSet"),
	    JOB("Job"),
	    CRONJOB("CronJob"),
	    CONFIGMAP("ConfigMap"),
	    SECRET("Secret"),
	    PERSISTENTVOLUME("PersistentVolume"),
	    PERSISTENTVOLUMECLAIM("PersistentVolumeClaim")
	};
	*/
}
