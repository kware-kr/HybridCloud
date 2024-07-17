package com.kware.rabbitmq.consumer;

import java.util.HashMap;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.rabbitmq.enable", havingValue = "true")
@RabbitListener(queues = { "${spring.rabbitmq.queue.policy_req.name}" }, concurrency = "1")
public class RabbitMQPolicyReqConsumer {

	@RabbitHandler
	public void consumePolicyReqMessage(Message msg) {

		log.info(String.format("Received policy request Message -> %s", new String(msg.getBody())));
		log.info(", user.getMessageProperties().getContentType(): {}", msg.getMessageProperties().getContentType());
	}

	@RabbitHandler
	public void consumePolicyReqMessage(String msg) {
		log.info(String.format("Received policy request String -> %s", msg));
	}

	//__typ__가 없으면 무조건 LinkedHashMap을 기본으로 가져가네.. 
	@RabbitHandler
	public void consumePolicyReqMessage(HashMap msg) {
		log.info(String.format("Received policy request HashMap -> %s", msg.toString()));
	}

	/*
	@RabbitListener(queues = {"${cetus.rabbitmq.queue.json.name}"}, concurrency = "1")
	public void consumeJsonMessage(User user) {
		log.info(String.format("Received Json Message -> %s", user.toString()));
	}
	*/
}
