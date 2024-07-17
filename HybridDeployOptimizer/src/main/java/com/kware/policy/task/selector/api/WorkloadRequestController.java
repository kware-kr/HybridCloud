package com.kware.policy.task.selector.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kware.common.util.JSONUtil;
import com.kware.common.util.YAMLUtil;
import com.kware.policy.common.QueueManager;
import com.kware.policy.service.vo.PromMetricNode;
import com.kware.policy.task.selector.service.WorkloadRequestService;
import com.kware.policy.task.selector.service.algo.BestFitBinPacking;
import com.kware.policy.task.selector.service.vo.WorkloadRequest;
import com.kware.policy.task.selector.service.vo.WorkloadResponse;
import com.kware.rabbitmq.publish.RabbitMQPolicyReqProducer;
import com.kware.rabbitmq.publish.RabbitMQPolicyResProducer;

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
public class WorkloadRequestController {
	
	private final WorkloadRequestService wrService;
	private final RabbitMQPolicyReqProducer reqProducer;
	private final RabbitMQPolicyResProducer resProducer;

	//추후 RabbitMQ에서 사용하도록 한다.
    @PostMapping("/ml/schedule/node_select")
    public ResponseEntity<?> processYaml(@RequestBody String yamlstring) {
        try {
        	
        	//{{rabbitmq에 송신 테스트
        	//reqProducer.sendYamlMessage(yamlstring);
        	reqProducer.sendMessage(yamlstring);
        	//}}
        	
        	WorkloadRequest wrRequest = YAMLUtil.read(yamlstring, WorkloadRequest.class);
        	wrRequest.aggregate();
            // 여기서는 단순히 변환된 YAML 문자열을 반환하나, 원하는 로직을 수행할 수 있습니다.
        	{
        		
        	}
        	
        	//{{ 단순 DB저장
        	WorkloadRequest.Request req = wrRequest.getRequest();
        	
        	//단순한 DB저장을 위해 원본 그대로를  json으로 변환하기 위함
        	req.setInfo(YAMLUtil.convertYamlToJson(yamlstring));
        	wrService.insertMoUserRequest(req);
        	//}}
        	
        	
        	WorkloadResponse wlResponse = new WorkloadResponse();
        	wlResponse.setVersion(WorkloadRequestService.interface_version);
        	
        	WorkloadResponse.Response res = new WorkloadResponse.Response();
        	res.setUid(req.getUid());
        	res.setId(req.getId());
        	res.setName(req.getName());
        	res.setClusterName("Workload-cluster-01");
        	res.setClusterId("1");
        	res.setNodeName("gpu-01");
        	res.setNodeId("0d0fa95a-1700-473c-bcf5-3ac44e764025");
        	res.setPreemptionPolicy(null);
        	res.setPriority(null);
        	res.setDate(new Date());
        	
        	wlResponse.setResponse(res);
        	res.setInfo(JSONUtil.getJsonstringFromObject(wlResponse));

        	String res_yamlstring = YAMLUtil.writeString(wlResponse);
        	System.out.println(res_yamlstring);
        	//{{rabbit-mq
        	//resProducer.sendYamlMessage(res_yamlstring);
        	resProducer.sendMessage(res_yamlstring);
        	//}}
        	
        	wrService.insertMoUserResponse(res);
        	
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process YAML");
        }
    }
    
    @PostMapping("/ml/schedule/node_score")
    public ResponseEntity<List<String>> getNodeScroe(@RequestBody String yamlstring) {
    	QueueManager qm = QueueManager.getInstance();
    	List<PromMetricNode> nodes = null;
    	WorkloadRequest wrRequest = null;
    	if(yamlstring == null) {
    		nodes = qm.getLastPromMetricNodesReadOnly();
    	}else {
    		wrRequest = YAMLUtil.read(yamlstring, WorkloadRequest.class);
        	wrRequest.aggregate();
        	nodes = qm.getAppliablePromMetricNodesReadOnly(wrRequest);
    	}
    	
    	//현재 특성 필터링은 안되었고, 스코어만 계산하는 것으로,
    	
    	ArrayList<String> rst = new ArrayList<String>();
    	double bestScore = 0;
    	PromMetricNode bestNode = null;
    	
    	String temp = null;
    	for(PromMetricNode node : nodes) {
    		double a = node.getScore();
    		
    		temp = "NodeScore Cluster ID:" + node.getClUid() + "	Node Name:" + node.getNode() + "	Score:" + a;
    		rst.add(temp);
    		
    		if(bestNode == null) {
    			bestNode = node;
    			bestScore = a;
    		}
    		
    		if(a > bestScore) {
    			bestNode = node;
    			bestScore = a;
    		}
    		
    		
    		/*
    		try {
				log.info("NodeScore: {} , {}", a, JSONUtil.getJsonstringFromObject(node));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
    		log.info("NodeScore: {} , cl: {}, node:[{}, {}]", a, node.getClUid(), node.getNode(), node.getNoUid());
    	}
    	
    	temp = "Selection Node => Cluster ID:" + bestNode.getClUid() + "	Node Name:" + bestNode.getNode() + "	Score:" + bestNode.getScore();
    	rst.add(temp);
    	
    	BestFitBinPacking bfbp = new BestFitBinPacking(nodes,qm.getNotApplyWorkloadRequestForNode());
    	List<PromMetricNode> bfbpList = bfbp.allocate(wrRequest);
    	
    	for(PromMetricNode node : bfbpList) {
    		temp = "BestFitBinPacking Cluster ID:" + node.getClUid() + "	Node Name:" + node.getNode();
    		rst.add(temp);
  
    		log.info("BestFitBinPacking: cl: {}, node:[{}, {}]", node.getClUid(), node.getNode(), node.getNoUid());
    	}
    	
    	if(wrRequest != null)
    		qm.setWorkloadRequest(wrRequest);
    	return ResponseEntity.ok(rst);
    }
}
