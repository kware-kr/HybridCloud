package com.kware.policy.task.common;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kware.policy.task.collector.service.vo.PromMetricPod;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.common.queue.WorkloadContainerQueue;
import com.kware.policy.task.common.vo.WorkloadCommand;
import com.kware.policy.task.selector.service.WorkloadRequestService;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.Container;
import com.kware.policy.task.selector.service.vo.WorkloadRequest.RequestWorkloadAttributes;
import com.kware.policy.task.selector.service.vo.WorkloadTaskWrapper;

@SuppressWarnings("rawtypes")
public class WorkloadCommandManager {
	private static final Logger queueLog = LoggerFactory.getLogger("queue-log");
    // Singleton 인스턴스
    private static final WorkloadCommandManager INSTANCE = new WorkloadCommandManager();

    // BlockingQueue와 작업 스레드
	private final BlockingQueue<WorkloadCommand> commandQueue;
    private final Thread workerThread;
    private volatile boolean running;
    private WorkloadRequestService wrService = null;
    
    QueueManager qm = QueueManager.getInstance();
    
    PromQueue    promQ = qm.getPromQ();
	RequestQueue requestQ = qm.getRequestQ();
	WorkloadContainerQueue wcQ = qm.getWorkloadContainerQ();
    
    
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
    
    public void setWorkloadRequestService(WorkloadRequestService service) {
    	this.wrService = service;
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
                		  || task.getCommand() == WorkloadCommand.CMD_WLD_EXPIRED) {
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
                	}else if(task.getCommand() == WorkloadCommand.CMD_CON_ENTER) {
                		//현재는 필요없는데..
                		
                	}else if(task.getCommand() == WorkloadCommand.CMD_POD_ENTER) {
                		//from: CollectorUnifiedPromMetricWorker
                		//process: wcQ
                		valObj = task.getValue();
                		
                		if(valObj instanceof PromMetricPod) {
                			PromMetricPod pod = (PromMetricPod)valObj;
                			wcQ.setPodInfo(pod);
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
}
