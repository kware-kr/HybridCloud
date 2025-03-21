package com.kware.policy.task.common.queue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import com.kware.policy.task.common.WorkloadCommandManager;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.selector.service.WorkloadRequestService;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역으로 사용할 스레드에 안전한 큐와 맵을 생성함
 * 요청 워크로드를 관리하고, 배포통지시간와, 쿠버테티스에 배포한 시간 정보만 갱신
 * 패포완료통지를 받았으나, api에 나타나지 않는 데이터를 삭제하기 위함 
 */
@Slf4j
public class RequestQueue  extends DefaultQueue{
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");

	//{{모든 요청된 request를 관리하고, api에서 사라지면 제거한다.
    //요청한 request 관리: key: WorkloadRequest.request.id ==> mlUid
    private final ConcurrentHashMap<String/*mlid*/, WorkloadRequest> requestMap;
    //}}요청관리
    
    WorkloadRequestService wrWervice = null;
    
  
    public RequestQueue() {
    	queueLog.info("Queue Log Start ====================================================="); //로그 파일 생성하는 목적
    	log.error("Error Log Start ====================================================="); //로그 파일 생성하는 목적
        this.requestMap         = new ConcurrentHashMap<String, WorkloadRequest>();
    }
    
    @Override
    public void setScheduler(TaskScheduler scheduler) {
        this.scheduler = scheduler;
        //스케줄러 생성
        this.createCleanSchedulerForRequestQueue();//requestMap clean
    }  
    
    //---------------------------------------------------------------------------------------------------

  	/**
     * 워크로드 요청 전체. (수정 불가) 
     * @return ReadOnly Map<String,WorkloadRequest>
     */
    public Map<String,WorkloadRequest> getWorkloadRequestReadOlnyMap() {
    	return Collections.unmodifiableMap(this.requestMap);    	
    }
  	
    /**
     * requestMap(요청) 사이즈
     * @return int
     */
  	public int getWorkloadRequestMapSize() {
  		return this.requestMap.size();
  	}

    public Integer getWorkloadContainerSize(String _mlId) {
    	WorkloadRequest req = requestMap.get(_mlId);
    	if(req == null) {
    		//DB에서 데이터 다시 가져오기
    		if(this.wrWervice != null)
    			return this.wrWervice.getWorkloadRequestContainerCount(_mlId);
    		else return 0;
    	}else {
    		return req.getRequest().getContainers().size();
    	}
    }

  	/** 
  	 * 노드선택 알고리즘을 적용한 후에 워크로드 등록하고 및 워크로드에 포함된 모든 컨테이너를 배포전의 노드관리에 등록한다.
  	 * @param _req
  	 */
    public void  setWorkloadRequest(WorkloadRequest _req) {
    	String mlId = _req.getRequest().getRequestKey();
		if (_req.getRequest().getClUid() == null || mlId == null) {
			throw new NullPointerException("clUid, node, request.id is nullable");
		}

		long timemilis = System.currentTimeMillis(); // 등록시간을 저장해서 나중에 expierd time을 적용하기 위함
		if (_req.getTimemillisecond() == 0L)
			_req.setTimemillisecond(timemilis);

		// 동일한 키가 있으면 값을 현재 버전으로 수정하고 이전 버전을 리턴한다.(내용물이 틀릴 수 있으므로 갱신한다.)
		this.requestMap.put(mlId, _req);
    }
    
    /**
     * workload mlId를 통해서 요청정보를 조회
     * @param _mlUid
     * @return
     */
    public WorkloadRequest getWorkloadRequest(String _mlUid) {
    	return this.requestMap.get(_mlUid);
    }
    
    /**
     * Queue에서 해당 워크로드 관련 정보 모두 제거
     * @param _mlUid
     */
    public void  reomveWorkloadRequest(String _mlUid) {
    	WorkloadRequest req = this.requestMap.get(_mlUid);
    	if(req != null) {
    		this.reomveWorkloadRequest(req);
    	}
    }
    
    /**
     * Queue에서 해당 워크로드 관련 정보 모두 제거
     * @param _req
     */
    public void  reomveWorkloadRequest(WorkloadRequest _req) {
   		this.requestMap.remove(_req.getRequest().getMlId());
    }
    
 
    /**
     * Pod명으로 요청당시의 container이름을 반환
     * @param _mlId
     * @param pod
     * @return 컨테이너 이름 또는 null
     */
    public String getContaienrnameFromPodname(String _mlId, String pod) {
    	
    	WorkloadRequest wlreq =  this.requestMap.get(_mlId);
    	List<Container> containerList = wlreq.getRequest().getContainers();
    	
    	for(Container c : containerList) {
    		String container_name = c.getName();
    		if(pod.contains(container_name)) { //pod 명은 컨테이너 이름을 포함해서 덧붙여진 이름
    			return container_name;
    		}
    	}
    	
    	return null;
    }
    	
    
    //통지완료가 없고, _miliseconds가 지난 데이터를 삭제하는 루틴
    public boolean isExpiredElements(String _mlId, long _expried_militime) {
    	WorkloadRequest element = this.requestMap.get(_mlId);
    	
    	if(element.getRequest().getDeployedAt() != null)  //이미 클러스터에 배포됨
    		return false;
    	
    	long cutoffTime = System.currentTimeMillis() - _expried_militime;
    	LocalDateTime notiDt = element.getRequest().getNotiDt();
    	long create_time = element.getTimemillisecond();
    	
    	if(notiDt == null) {
    		if(create_time < cutoffTime) {
    	        if(queueLog.isDebugEnabled()) {
    	        	queueLog.debug("RequestMap: remove mlId: {}, 나머지 갯수:{}", element.getRequest().getMlId(), requestMap.size() );
    	        }
    	        return true;
    		}
    	}
    	return false;
    }
    
    long expired_time = 2 * 60 * 60 * 1000;//2시간 
    //내부 스케줄링을 통해 제거작업등을 수행하도록 함
    //1.시간지난 promDeques 정리작업 진행
	private void createCleanSchedulerForRequestQueue() {
	
		// 5분마다 수행할 작업 정의
		Runnable periodicTask = new Runnable() {
			@Override
			public void run() {
				Iterator<String> iterator = requestMap.keySet().iterator();
				while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
					String mlId = iterator.next();
					if(isExpiredElements(mlId, expired_time)) {
						
						//WraperQueue에서 제거
						WorkloadCommand<String> command = new WorkloadCommand<String>(WorkloadCommand.CMD_WLD_EXPIRED,mlId);
						WorkloadCommandManager.addCommand(command);
						
						iterator.remove();
					}
				}
			}
		};
		
		// 초기 지연 시간 없이 5분마다 작업을 실행
		scheduler.scheduleAtFixedRate(periodicTask, Instant.now(), Duration.ofMinutes(5)); // 처음 실행 시간을 약간 지연시킬 수 있음
	             // 5분 간격 (밀리초));
	}

	public void setWrWervice(WorkloadRequestService wrWervice) {
		this.wrWervice = wrWervice;
	}
}