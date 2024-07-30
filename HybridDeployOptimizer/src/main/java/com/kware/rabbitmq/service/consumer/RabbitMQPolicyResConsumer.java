package com.kware.rabbitmq.service.consumer;

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
@RabbitListener(queues = { "${spring.rabbitmq.queue.policy_res.name}" }, concurrency = "1")
public class RabbitMQPolicyResConsumer {

	@RabbitHandler(isDefault = true)
	public void consumePolicyResMessage(Message msg) {
		log.info(String.format("Received policy response Message -> %s", new String(msg.getBody())));
		log.info(", user.getMessageProperties().getContentType(): {}", msg.getMessageProperties().getContentType());
	}

	
	@RabbitHandler
	public void consumePolicyResMessage(String msg, Message message) {
		log.info(String.format("Received policy response String -> %s", msg));
		log.info(", user.getMessageProperties().getContentType(): {}", message.getMessageProperties().getContentType());
	}

	//__typ__가 없으면 무조건 LinkedHashMap을 기본으로 가져가네.. 
	@RabbitHandler
	public void consumePolicyResMessage(HashMap msg, Message message) {
		log.info(String.format("Received policy response HashMap -> %s", msg.toString()));
		log.info(", user.getMessageProperties().getContentType(): {}", message.getMessageProperties().getContentType());
	}

	
	/*
	@RabbitListener(queues = {"${cetus.rabbitmq.queue.json.name}"}, concurrency = "1")
	public void consumePolicyResMessage(User user) {
		log.info(String.format("Received Json Message -> %s", user.toString()));
	}
	*/

}
