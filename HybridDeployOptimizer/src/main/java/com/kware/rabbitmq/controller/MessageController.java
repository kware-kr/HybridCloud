package com.kware.rabbitmq.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kware.rabbitmq.service.producer.RabbitMQPolicyReqProducer;
import com.kware.rabbitmq.service.producer.RabbitMQPolicyResProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


//여기는 테스트 용도임

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

	private final RabbitMQPolicyResProducer resProducer;
	private final RabbitMQPolicyReqProducer reqProducer;

	
	//////////////////////////////request queue /////////////////////////////////////////////
	
	@RequestMapping(value="/req/str/publish", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<String> sendReqMessage(@RequestParam("message") String message) {
		log.info("Request String Message sent to RabbitMQ ...");
		reqProducer.sendMessage(message);
        return ResponseEntity.ok("Request String Message sent to RabbitMQ ...");
    }
	
	@PostMapping("/req/json/publish")
    public ResponseEntity<String> sendJsonReqMessage(@RequestBody String message) {
		log.info("Request Json Message sent to RabbitMQ ...");
		reqProducer.sendJsonMessage(message);
        return ResponseEntity.ok("Request  Json Message sent to RabbitMQ ...");
    }
	
	@PostMapping("/req/yaml/publish")
    public ResponseEntity<String> sendYamlReqMessage(@RequestBody String message) {
		log.info("Request Yaml Message sent to RabbitMQ ...");
		reqProducer.sendYamlMessage(message);
        return ResponseEntity.ok("Request Yaml Message sent to RabbitMQ ...");
    }
	
	
	////////////////////////////// response queue /////////////////////////////////////////////
	
	@RequestMapping(value="/res/str/publish", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<String> sendResMessage(@RequestParam("message") String message) {
		log.info("Response String Message sent to RabbitMQ ...");
		resProducer.sendMessage(message);
        return ResponseEntity.ok("Response String Message sent to RabbitMQ ...");
    }
	
	@PostMapping("/res/json/publish")
    public ResponseEntity<String> sendJsonResMessage(@RequestBody String message) {
		log.info("Response Json Message sent to RabbitMQ ...");
		resProducer.sendJsonMessage(message);
        return ResponseEntity.ok("Response Json Message sent to RabbitMQ ...");
    }
	
	@PostMapping("/res/yaml/publish")
    public ResponseEntity<String> sendYamlResMessage(@RequestBody String message) {
		log.info("Response Yaml Message sent to RabbitMQ ...");
		resProducer.sendYamlMessage(message);
        return ResponseEntity.ok("Response Yaml Message sent to RabbitMQ ...");
    }
	
}