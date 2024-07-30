package com.kware.policy.task.selector.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.util.StringUtil;
import com.kware.common.util.YAMLUtil;
import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.common.QueueManager;
import com.kware.policy.task.common.queue.PromQueue;
import com.kware.policy.task.common.queue.RequestQueue;
import com.kware.policy.task.selector.service.WorkloadRequestService;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadResponse;
import com.kware.rabbitmq.service.producer.RabbitMQPolicyReqProducer;
import com.kware.rabbitmq.service.producer.RabbitMQPolicyResProducer;
import com.kware.rabbitmq.service.vo.RMQCommandIFv1;
import com.kware.rabbitmq.service.vo.RMQCommandIFv1.Header;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 테스트를 위해서 진행함
1차 바로진행
2창 여기에서 rabbit mq로 보내고, rabbit mq에서 받아서 처리하고, mq에서 다시 보내고, 받는 형태로 처리
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WorkloadRequestRestController {
	
	private final WorkloadRequestService    wlService;
	private final RabbitMQPolicyReqProducer reqProducer;
	private final RabbitMQPolicyResProducer resProducer;

	QueueManager qm = QueueManager.getInstance();
	//APIQueue     apiQ     = qm.getApiQ();
	PromQueue    promQ    = qm.getPromQ();
	RequestQueue requestQ = qm.getRequestQ();
	
	//추후 RabbitMQ에서 사용하도록 한다.
	/**
	 * 배포가능한 최적의 노드를 조회(BestFitBinPacking)
	 * @param yamlstring
	 * @return
	 */
    @PostMapping("/ml/schedule/node_select")
    public ResponseEntity<?> getNodeBFBP(@RequestBody String yamlstring) {
        try {
        	//{{rabbitmq에 송신 테스트
        	//reqProducer.sendYamlMessage(yamlstring);
        	reqProducer.sendMessage(yamlstring);
        	//}}
        	
        	WorkloadRequest wlRequest = YAMLUtil.read(yamlstring, WorkloadRequest.class);
        	wlRequest.aggregate();
            
        	//{{ WorkloadRequest DB저장
        	WorkloadRequest.Request req = wlRequest.getRequest();
        	//단순한 DB저장을 위해 원본 그대로를  json으로 변환하기 위함
        	req.setInfo(YAMLUtil.convertYamlToJson(yamlstring));
        	wlService.insertMoUserRequest(req);
        	//}}
        	
        	//{{노드 셀렉터
        	WorkloadResponse wlResponse = wlService.getNodesSelector(wlRequest);
        	//}}
        	
        	//DB 저장
        	wlService.insertMoUserResponse(wlResponse.getResponse());
        	
        	//WorkloadRequest에 Response 입력
        	wlRequest.setResponse(wlResponse.getResponse());

        	//{{rabbit-mq
        	String res_yamlstring = YAMLUtil.writeString(wlResponse);
        	//resProducer.sendYamlMessage(res_yamlstring);
        	resProducer.sendMessage(res_yamlstring);
        	//}}

        	return ResponseEntity.ok(wlRequest);
            //return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process YAML");
        }
    }
    
    /**
     * RabbitMQ에 보내도록 노드 선택 요청하기 위한 인터페이스
     * @param String ymlstring
     * @param WorkloadRequest wlReq
     */
    private void sendReqRabbitMQ_node_request(String ymlstring, WorkloadRequest wlReq) {
    	RMQCommandIFv1 cmdIf = new RMQCommandIFv1();
    	Header header = cmdIf.getHeader();
    	header.setMsg_id(wlReq.getRequest().getId());
    	header.setLocation("keti");
    	header.setMsg_kind("req");
    	header.setMsg_type("node_request");
    	header.setTimestamp(StringUtil.getToday());
    	RMQCommandIFv1.Body body = cmdIf.getBody();
    	body.setEncoding("base64");
    	body.setDecodedContents(ymlstring);
    	
    	try {
			reqProducer.sendMessage(cmdIf.toJson());
		} catch (Exception e) {
			log.error("RabbitMQ send Error", e);
		}
    }
    
    /**
     * 배포가능한 노드의 전체 스코어 BestFitBinPacking 알고리즘 적용한 계산
     * @param yamlstring
     * @return
     */
    @PostMapping("/ml/schedule/node_score_bfbp")
    public ResponseEntity<?> getNodeScoreBFBP(@RequestBody String yamlstring) {
        try {
        	
        	//{{rabbitmq에 송신 테스트
        	//reqProducer.sendYamlMessage(yamlstring);
        	reqProducer.sendMessage(yamlstring);
        	//}}
        	
        	WorkloadRequest wlRequest = YAMLUtil.read(yamlstring, WorkloadRequest.class);
        	wlRequest.aggregate();
            
        	//{{ WorkloadRequest DB저장
        	WorkloadRequest.Request req = wlRequest.getRequest();
        	//단순한 DB저장을 위해 원본 그대로를  json으로 변환하기 위함
        	req.setInfo(YAMLUtil.convertYamlToJson(yamlstring));
        	wlService.insertMoUserRequest(req);
        	//}}
        	
        	//{{노드 셀렉터
        	List<PromMetricNode> sel_nodes = wlService.getNodeScore(wlRequest, 0); //전체 노드의 순서
        	//}}
        	        	
        	return ResponseEntity.ok(sel_nodes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process YAML");
        }
    }

    /**
     * 단순한 용량으로 스코어 계산
     * @param yamlstring
     * @return
     */
    @PostMapping("/ml/schedule/node_score_for_capacity")
    public ResponseEntity<?> getNodeScroeCapacity(@RequestBody String yamlstring) {
    	List<PromMetricNode> nodes = null;
    	WorkloadRequest wlRequest = null;
    	if(yamlstring == null) {
    		nodes = promQ.getLastPromMetricNodesReadOnly();
    	}else {
    		wlRequest = YAMLUtil.read(yamlstring, WorkloadRequest.class);
        	wlRequest.aggregate();
        	nodes = promQ.getAppliablePromMetricNodesReadOnly(wlRequest);
    	}
    	
    	for(PromMetricNode node : nodes) {
    		double a = node.getScore();
    		node.setBetFitScore(a);
    		log.info("NodeScore: {} , cl: {}, node:[{}, {}]", a, node.getClUid(), node.getNode(), node.getNoUid());
    	}

    	return ResponseEntity.ok(nodes);
    }
}
