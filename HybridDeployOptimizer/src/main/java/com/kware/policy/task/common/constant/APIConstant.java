package com.kware.policy.task.common.constant;

public class APIConstant {
	
	public static enum metricMapAtt{
		CLUID, PRQLUID, RESULT, EXTRACTPATH
	};
	
	public static enum WorkloadStatus{
		Started, Finished, Error, Running, Pending
	};
	
	public static String API_CLUSTER_PROMETHEUSURL = "/interface/api/v2/ml/cluster/{clusterId}/prometheusUrl"; //GET : Prometheus URL
	public static String API_CLUSTER               = "/interface/api/v2/ml/cluster/{clusterIdx}";              //GET : cluster 상세조회(prometheus url 있음)
	public static String API_CLUSTER_LIST          = "/interface/api/v2/ml/cluster/list";                      //GET : cluster 리스트
	public static String API_CLUSTER_SCALE         = "/interface/api/v2/ml/cluster/scale";                     //POST: cluster Scale 조정(node 생성관련) 
	
	public static String API_ML_LIST   = "/interface/api/v2/ml/ml/list";       //POST:   ML Workload 리스트 조회
	public static String API_ML        = "/interface/api/v2/ml/{mlId}";        //GET:    ML Workload 상세조회
	public static String API_ML_APPLY  = "​/interface​/api​/v2​/ml​/apply";         //POST:   ML Workload 시작 
	public static String API_ML_FINISH = "/interface/api/v2/ml/finish/{mlId}"; //PUT:    Workload 완료
	public static String API_ML_DELETE = "/interface/api/v2/ml/delete";        //DELETE: ML Workload 중지 및 삭제
	
	//API
	public final static String API_RESULT_CODE_OK = "10001";
	
	
	//정책요청을 위한 인터페이스 버전
	public final static String POLICY_INTERFACE_VERSION = "0.5";
	
	
	//request uri 관련(KETI)
	public static String REQUEST_SCALE = "/submit_resource"; //재배포 요청 URI
}
