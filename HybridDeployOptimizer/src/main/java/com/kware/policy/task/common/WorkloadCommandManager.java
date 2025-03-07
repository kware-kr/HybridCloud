package com.kware.policy.task.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.common.queue.APIQueue;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;
import com.kware.policy.task.common.service.CommonService;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.feature.FeatureMain;
import com.kware.policy.task.feature.eval.GpuPerformanceEvaluator;
import com.kware.policy.task.feature.eval.NodePerformanceEvaluator;
import com.kware.policy.task.feature.eval.SecurityLevelEvaluator;
import com.kware.policy.task.feature.service.vo.ClusterNodeFeature;
import com.kware.policy.task.scalor.service.ScalingInfoService;
import com.kware.policy.task.scalor.service.vo.NodeScalingInfo;
import com.kware.policy.task.scalor.service.vo.PodScalingInfo;
import com.kware.policy.task.selector.service.WorkloadRequestService;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.RequestWorkloadAttributes;
import com.kware.policy.task.selector.service.vo.WorkloadTaskWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("rawtypes")
public class WorkloadCommandManager {
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
	private static final Logger scaleLog = LoggerFactory.getLogger("scale-log");
	
	private final AtomicInteger threadNumber = new AtomicInteger(1); // 스레드 번호를 위한 카운터
    private final String namePrefix = "ScalingInfoThread-"; // 프리픽스 설정
    
    // Singleton 인스턴스
    private static final WorkloadCommandManager INSTANCE = new WorkloadCommandManager();

    // BlockingQueue와 작업 스레드
	private final BlockingQueue<WorkloadCommand> commandQueue;
    private final Thread workerThread;
    private volatile boolean running;
    
    private WorkloadRequestService wrService = null;
    private CommonService comService = null;
    private FeatureMain feaMain = null;
    private ScalingInfoService scalingService = null;
    
    QueueManager qm = QueueManager.getInstance();
    
    PromQueue    promQ = qm.getPromQ();
	RequestQueue requestQ = qm.getRequestQ();
	WorkloadContainerQueue wcQ = qm.getWorkloadContainerQ();
	APIQueue apiQ = qm.getApiQ();
    
    // Singleton private 생성자
    private WorkloadCommandManager() {
        this.commandQueue = new LinkedBlockingQueue<>();
        this.running = true;

        // 작업 스레드
        this.workerThread = new Thread(new Worker());
        this.workerThread.start();
    }

    // Singleton 인스턴스 반환
    public static WorkloadCommandManager getInstance() {
        return INSTANCE;
    }

    // Command 추가
    public static void addCommand(WorkloadCommand command) {
        getInstance().enqueue(command);
    }

    // 큐에 작업 추가
    private void enqueue(WorkloadCommand command) {
        if (!running) {
            throw new IllegalStateException("CommandQueue is shutting down. Cannot accept new commands.");
        }
        commandQueue.offer(command); // BlockingQueue에 추가
    }
    
    /*
    public void setWorkloadRequestService(WorkloadRequestService service) {
    	this.wrService = service;
    }
    
    public void setCommonService(CommonService comService) {
		this.comService = comService;
	}
	*/
    
    public void setInitSevice(WorkloadRequestService wrService, CommonService comService, FeatureMain fmain, ScalingInfoService scalingService){
    	this.wrService      = wrService;
    	this.comService     = comService;
    	this.feaMain        = fmain;
    	this.scalingService = scalingService;
    }

    // 큐를 종료하고 스레드를 중지
    public void shutdown() {
        running = false;
        workerThread.interrupt(); // 스레드 깨움
    }

    // CommandQueue 실행 상태 확인
    public boolean isRunning() {
        return running;
    }

    // 내부 클래스: 작업 스레드의 동작 정의
    private class Worker implements Runnable {
        @SuppressWarnings("unchecked")
		@Override
        public void run() {
            while (running) {
                try {
                	Object valObj = null;
                	
                    // 작업 가져오기 (큐가 비어있으면 대기)
                	WorkloadCommand<?> task = commandQueue.take();
                	/*
                	WorkloadCommand<?> task = commandQueue.poll(500, TimeUnit.MILLISECONDS);
                	if(task == null) {
                		continue;
                	}
                	*/
                	
                	//워크로드 입력요청
                	if(task.getCommand() == WorkloadCommand.CMD_WLD_ENTER) {
                		//from: WorkloadRequestService
                		//process: wcQ
                		valObj = task.getValue();
                		if(valObj instanceof List ) {
                			List<WorkloadTaskWrapper> req = (List<WorkloadTaskWrapper>)valObj;
                			wcQ.setWorkloadTaskWrappers(req);
                		}else if(valObj instanceof WorkloadRequest.Request ) { //미사용
                			WorkloadRequest.Request req = (WorkloadRequest.Request)valObj;
                			List<Container> c = req.getContainers();
                			RequestWorkloadAttributes a = req.getAttribute();
                			wcQ.setWorkloadTaskContainers(a, c);
                		}
                	}else if(task.getCommand() == WorkloadCommand.CMD_WLD_COMPLETE   
                		  || task.getCommand() == WorkloadCommand.CMD_WLD_EXPIRED) {//워크로드 완료
                		//from complete:    CollectorWorkloadApiWorker
                		//from expired:    requestQueue
                		//process: requestQ, wcQ제거, WorkloadRequestService DB 서비스
                		
                		valObj = task.getValue();
                		if(valObj instanceof String ) {
                			String mlId = (String)task.getValue();
                			wrService.updateUserRequest_complete(mlId);
                			
                			requestQ.reomveWorkloadRequest(mlId);
                			wcQ.removeWorkloadTaskContainer(mlId);
                		}
                	}else if(task.getCommand() == WorkloadCommand.CMD_CON_ENTER) { // 컨테이너 등록처리
                		//현재는 필요없는데..
                		
                	}else if(task.getCommand() == WorkloadCommand.CMD_POD_ENTER) { ////워크로드 파드 등록 처리
                		//from: CollectorUnifiedPromMetricWorker
                		//process: wcQ
                		valObj = task.getValue();
                		
                		if(valObj instanceof PromMetricPod) {
                			PromMetricPod pod = (PromMetricPod)valObj;
                			wcQ.setPodInfo(pod);
                		}                		
                	}else if(task.getCommand() == WorkloadCommand.CMD_NODE_CHANGE) {
                		//성능들을 생성함
                		generatePerformance();
                	}else if(task.getCommand() == WorkloadCommand.CMD_NODE_SCALING_IN) { //노드스케일링
                		//노드 줄이는 요청, 스레드 처리
                		valObj = task.getValue();
                		
                		List<NodeScalingInfo> nodeScalingInfos = null;
                		if(valObj instanceof List) {
                			nodeScalingInfos = (List<NodeScalingInfo>)valObj;
                			scalingService.processNodeScaling(nodeScalingInfos);
                		}
                		if(scaleLog.isInfoEnabled())
                			scaleLog.info("노드 스케일링 IN 요청[CMD_NODE_SCALING_IN]:\n{}", nodeScalingInfos);

                	}else if(task.getCommand() == WorkloadCommand.CMD_NODE_SCALING_OUT) {
                		//노드 늘리는 요청, 스레드 처리
                		valObj = task.getValue();
                		
                		List<NodeScalingInfo> nodeScalingInfos = null;
                		if(valObj instanceof List) {
                			nodeScalingInfos = (List<NodeScalingInfo>)valObj;
                			scalingService.processNodeScaling(nodeScalingInfos);
                		}
                		if(scaleLog.isInfoEnabled())
                			scaleLog.info("노드 스케일링 OUT 요청[CMD_NODE_SCALING_OUT]:\n{}", nodeScalingInfos);

                	}else if(task.getCommand() == WorkloadCommand.CMD_POD_SCALING) {
                		//파드 리소스 조정 요청, 스레드 처리
                		valObj = task.getValue();
                		
                		if(scaleLog.isInfoEnabled())
                			scaleLog.info("파드 스케일링 요청[CMD_POD_SCALING]:\n{}", valObj);
                		
                		PodScalingInfo psInfo;
                		if(valObj instanceof PodScalingInfo) {
                			psInfo = (PodScalingInfo)valObj;
                			WorkloadRequest wr = requestQ.getWorkloadRequest(psInfo.promMetricPod.getMlId());
                			
                			ThreadGroup scalingGroup = new ThreadGroup("ScalingServiceGroup");
                			
                			Thread thread = new Thread(scalingGroup, () -> {
                				try {
                					scalingService.requestScalingApiCall(psInfo, wr);
                			        psInfo.clear();
                				}catch(Exception e) {
                					scaleLog.error("API call to notify the Optimizer server of configuration changes failed. Error:", e);
                				}
                			}, namePrefix + threadNumber.getAndIncrement());
                			thread.start();
                			
                		}
                    }
                	
                	queueLog.debug("Wrokload worker: command {}",task.getCommand());
                	wcQ.debuglog();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // 종료 처리
                }catch(Exception e) {
                	queueLog.error("WorkloadQue take Error: {}", e);
                	continue;
                }
            }
        }
    }
    
    private void generatePerformance() {
    	List<PromMetricNode> lastNodes =  promQ.getLastPromMetricNodesReadOnly();
    	if(lastNodes == null)
    		return;
    	
    	
    	//GPU
    	GpuPerformanceEvaluator gpfe = new GpuPerformanceEvaluator();
    	gpfe.setCommonService(comService);
    	Map<String, Integer> gpfeRankMap = gpfe.getFormance(lastNodes);
    	
    	//성능
    	NodePerformanceEvaluator npfe = new NodePerformanceEvaluator();
    	Map<String, Integer> npfeRankMap = npfe.getFormanceScore(lastNodes);
    	
    	//보안
    	SecurityLevelEvaluator sle = new SecurityLevelEvaluator();
    	Map<String, Integer> sleRankMap = sle.calculateSecurityLevel(lastNodes, feaMain.getClusterFeature());
    	
    	
    	ClusterNodeFeature nf = null;
    	List<ClusterNodeFeature> nodeFeatures = new ArrayList<ClusterNodeFeature>(); 
    	for(PromMetricNode n: lastNodes) {
    		nf = new ClusterNodeFeature();
    		nf.setClUid(n.getClUid());
    		nf.setNodeName(n.getNode());
    		nf.setGpuLevel        (gpfeRankMap.get(n.getKey()));
    		nf.setPerformanceLevel(npfeRankMap.get(n.getKey()));
    		nf.setSecurityLevel   (sleRankMap.get(n.getKey()));
    		
    		nodeFeatures.add(nf);
    	}
    	
    	feaMain.setClusterNodeFeature(nodeFeatures);
    	
    	//System.out.println(gpfeRankMap);
    	//System.out.println(npfeRankMap);
    	//System.out.println(sleRankMap);
    	
    	gpfeRankMap.clear();
    	npfeRankMap.clear();
    	sleRankMap.clear();
//    	String a = "aaa";
    	
    }
}
