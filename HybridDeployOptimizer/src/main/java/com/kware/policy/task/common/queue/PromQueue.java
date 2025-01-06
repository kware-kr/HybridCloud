package com.kware.policy.task.common.queue;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import com.kware.policy.task.collector.service.vo.PromMetricDefault;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodes;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.collector.service.vo.PromMetricPods;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * 프로메테우스에서 수집한 데이터를 전역 관리
 * 스레드에 안전한 큐와 맵을 생성함
 * @param <T>
 */
@Slf4j
@SuppressWarnings({"unchecked","rawtypes"})
public class PromQueue extends DefaultQueue{
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");

    //{{Prometheus
    //수집한 메트릭을 기반으로 처리함: nodeinfo는 스케줄링을 위한, pod는 스케일링을 위한 용도 사용
    public static enum PromDequeName{   	METRIC_NODEINFO, METRIC_PODINFO    };
    //PromDequeName별도 다른 Object를 BlockingDeque에 관리한다.,PromMetricNodes, PromMetricPods
    private final Map<PromDequeName, BlockingDeque<?>> promDequesMap; //프로메테우스 metric 저장, Node저장 deque, pod저장 deque
    //}}Prometheus
     
    public PromQueue() {
    	queueLog.info("Queue Log Start ====================================================="); //로그 파일 생성하는 목적
    	log.error("Error Log Start ====================================================="); //로그 파일 생성하는 목적

        this.promDequesMap = new HashMap<>();
    }
    
    @Override
    public void setScheduler(TaskScheduler scheduler) {
        this.scheduler = scheduler;
        //스케줄러 생성
        this.createClenaSchedulerForPromQueue();
    }  
    
    
    //{{ //////////////deque LIFO, FIFO모두 사용가능 ==> LIFO로 사용하자. //////////////////////
    
    private BlockingDeque<?> getPromDeque(PromDequeName name) {
        return this.promDequesMap.computeIfAbsent(name, k -> new LinkedBlockingDeque<>());
    }
    
    /**
	 * PromMetricNodes class를 관리하는 BlockingDeque
	 * @return
	 */
//	public BlockingDeque<PromMetricNodes> getPromNodesDeque() {
//		return (BlockingDeque<PromMetricNodes>)getPromDeque(PromDequeName.METRIC_NODEINFO);
//	}
	
		/**
	 * PromMetricPods class를 관리하는 BlockingDeque
	 * @return
	 */
//	public BlockingDeque<PromMetricPods> getPromPodsDeque() {
//		return (BlockingDeque<PromMetricPods>)getPromDeque(PromDequeName.METRIC_PODINFO);
//	}
	
	public int getProDequeSize(PromDequeName name) {
		BlockingDeque<?> deque = this.promDequesMap.get(name);
    	if(deque == null) 
    		return 0;
    		
    	return deque.size();
	}
  	//-----------------------------------------------------------------------------------------------
  	
    public int getPromDequesSize(PromDequeName name) {
    	BlockingDeque deque = this.promDequesMap.get(name);
    	if(deque == null) 
    		return 0;
    		
    	return deque.size();
    }

	public void addPromDequesObject(PromDequeName name, Object obj) {
		BlockingDeque deque = this.promDequesMap.get(name);
    	if(deque == null) {
    		deque = getPromDeque(name);
    	}
    	deque.addFirst(obj);
    }
    
    public Object getPromDequesFirstObject(PromDequeName name) {
    	BlockingDeque<?> deque = this.promDequesMap.get(name);
    	if(deque == null)
    		return null;
    	else return deque.peekFirst();
    }
    
    public void clearePromDeques(PromDequeName name) {
    	BlockingDeque<?> q = this.promDequesMap.remove(name);
    	if(q == null) return;
    	
        Iterator<?> iterator = q.iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
            PromMetricDefault element = (PromMetricDefault)iterator.next();
            element.clear();
            iterator.remove();
        }        
    }

    /**
     *  모든 큐를 제거하는 메서드: 
     *  여기는 실제 종료시점이므로 불필요함
     */
    public void removeAllDeque() {
    	Iterator<PromDequeName> iterator = this.promDequesMap.keySet().iterator();
        while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
        	clearePromDeques(iterator.next());
        }
    }
    
    /**
     * Metric을 통해 수집한 최신 읽기전용 Node 정보를 제공  
     * @param name
     * @return ReadOnly List<PromMetricNode>
     */
    
    public List<PromMetricNode> getLastPromMetricNodesReadOnly() {
		BlockingDeque<PromMetricNodes> nodeDeque = (BlockingDeque<PromMetricNodes>)this.promDequesMap.get(PromDequeName.METRIC_NODEINFO);
    	PromMetricNodes  nodes= nodeDeque.peekFirst();
    	if(nodes == null)
    		return null;
    	
    	return nodes.getUnmodifiableAllNodeList();
    }
    
    /**
     * 배포가능한 노드 리스트 제공(배포할 수 없는 노드는 제외)- master 등
     * @return ReadOnly List<PromMetricNode>
     */
    public List<PromMetricNode> getAppliablePromMetricNodesReadOnly() {
    	BlockingDeque<PromMetricNodes> nodeDeque = (BlockingDeque<PromMetricNodes>)this.promDequesMap.get(PromDequeName.METRIC_NODEINFO);
    	PromMetricNodes  nodes= nodeDeque.peekFirst();
    	
    	if(nodes == null)
    		return null;
    	return nodes.getUnmodifiableAppliableNodeList();
    }
    
    /**
     * Metric에서 수집된 다양한 최신 읽기전용 POD 데이터를 조회
     * @return ReadOnly List<PromMetricPod>
     */
    public List<PromMetricPod> getLastPromMetricPodsReadOnly() {
    	BlockingDeque<PromMetricPods> deque = (BlockingDeque<PromMetricPods>)this.promDequesMap.get(PromDequeName.METRIC_PODINFO);
    	PromMetricPods pods = deque.peekFirst();
    	
    	if(pods == null)
    		return null;
    	
    	return pods.getUnmodifiableAllPodList();
    }
    //}}
    
    //_miliseconds가 지난 데이터를 삭제하는 루틴
    public void removeExpiredDequeElements(PromDequeName name, long _miliseconds) {
    	
    	long cutoffTime = System.currentTimeMillis() - _miliseconds;
    	BlockingDeque<?> q = this.promDequesMap.get(name);
    	
    	
    	//{{데이터 로그 생성
    	if(queueLog.isDebugEnabled()) {
    		queueLog.debug("**********************************************************************************");
    		queueLog.debug("Deque-{}: remove before: size: {}",name,  q.size());

    		Object obj = q.peekFirst();
    		if(obj instanceof PromMetricNodes) {
    			PromMetricNodes nodeList = (PromMetricNodes)obj;
    			for(PromMetricNode n: nodeList.getAllNodeList()) {
    				queueLog.debug("========================");
    				queueLog.debug("PromMetricNode: {}", n);
    				queueLog.debug("========================");
    			}
    		}else if(obj instanceof PromMetricPods) {
    			PromMetricPods podList = (PromMetricPods)obj;
    			for(PromMetricPod n: podList.getAllPodList()) {
    				queueLog.debug("++++++++++++++++++++++++");
    				queueLog.debug("PromMetricPod: {}", n);
    				
    				n.getMContainerList().forEach((key, value) -> {
    					queueLog.debug("---------------------------");
    					queueLog.debug("PromMetricContainer: {}", value);
    					queueLog.debug("---------------------------");
    				});
    				queueLog.debug("++++++++++++++++++++++++");
    			}
    		}
    	}
    	    			
    	Iterator<?> iterator = q.descendingIterator();
        
    	//오래된 데이터 제거
        while (iterator.hasNext()) {
        	PromMetricDefault element = (PromMetricDefault)iterator.next();
            if (element.getTimemillisecond() < cutoffTime) {
            	element.clear();
                iterator.remove();
            } else {
                break; // 타임스탬프가 더 최근인 경우, 더 이상 뒤로 갈 필요 없음
            }
        }
        
        if(queueLog.isDebugEnabled()) {
        	queueLog.debug("Deque-{}: remove after: size: {}",name, q.size());
        	queueLog.debug("**********************************************************************************");
        }
    }
    
    long expired_time = 30 * 60 * 1000;
    //내부 스케줄링을 통해 제거작업등을 수행하도록 함
    //1.시간지난 promDeques 정리작업 진행
	private void createClenaSchedulerForPromQueue() {
		// 10분마다 수행할 작업 정의
		Runnable periodicTask = new Runnable() {
			@Override
			public void run() {
				Iterator<PromDequeName> iterator = promDequesMap.keySet().iterator();
				while (iterator.hasNext()) { // 큐 내부의 모든 요소를 순회하면서 제거
					removeExpiredDequeElements(iterator.next(), expired_time);
				}
			}
		};

		// 초기 지연 시간 없이 5분마다 작업을 실행
		scheduler.scheduleAtFixedRate(periodicTask, Instant.now(), Duration.ofMinutes(5)); // 처음 실행 시간을 약간 지연시킬 수 있음
			             // 5분 간격 (밀리초));
	}
}