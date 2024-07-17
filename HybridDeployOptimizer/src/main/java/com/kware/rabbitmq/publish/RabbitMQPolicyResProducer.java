package com.kware.rabbitmq.publish;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kware.rabbitmq.config.RabbitMQConfig;
import com.kware.rabbitmq.message.YamlMessageConverter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RabbitMQPolicyResProducer {

	private final RabbitTemplate rabbitTemplate;
	
	@Value("${spring.rabbitmq.exchange.policy.name}")
	private String exchange;
	
	@Value("${spring.rabbitmq.routing.policy_res.key}")
	private String routingKey;
	
	@Qualifier("policy_message_converter")
	private ContentTypeDelegatingMessageConverter deleatingMsgConverter; //

	public RabbitMQPolicyResProducer(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void sendMessage(String msg) {
		log.info(String.format("Response String Message send -> %s", msg));
		rabbitTemplate.convertAndSend(exchange, routingKey, msg);
	}
	
	public void sendYamlMessage(Object msg) {
		if(msg instanceof String) {
			log.info(String.format("Response Yaml String Message send -> %s", msg));
			MessageProperties messageProperties = new MessageProperties();
			messageProperties.setContentType(RabbitMQConfig.contentTypeYAML);//이부분이 있으면 HashMap으로 수신, 없으면, Message로 수신
			Message message = new Message(((String)msg).getBytes(), messageProperties);
			rabbitTemplate.send(exchange, routingKey, message);
		}else {
			log.info(String.format("Response Yaml Object Message send -> %s", msg.toString()));

			//ContentTypeDelegatingMessageConverter converter = (ContentTypeDelegatingMessageConverter) rabbitTemplate.getMessageConverter();
			YamlMessageConverter yamlMessageConverter = (YamlMessageConverter)deleatingMsgConverter.getDelegates()
					.get(RabbitMQConfig.contentTypeYAML);
	
			// Object를 JSON으로 변환하여 Message 객체를 생성합니다.
			Message message = yamlMessageConverter.toMessage(msg, new MessageProperties());
	
			rabbitTemplate.send(exchange, routingKey, message);
		}
	}

	public void sendJsonMessage(Object msg) {
		
		if(msg instanceof String) {
			log.info(String.format("Response Json String Message send -> %s", msg));
			MessageProperties messageProperties = new MessageProperties();
			messageProperties.setContentType(RabbitMQConfig.contentTypeJSON);
			Message message = new Message(((String)msg).getBytes(), messageProperties);
			rabbitTemplate.send(exchange, routingKey, message);
		}else {
			log.info(String.format("Response Json Object Message send -> %s", msg.toString()));
	
			//ContentTypeDelegatingMessageConverter converter = (ContentTypeDelegatingMessageConverter) rabbitTemplate.getMessageConverter();
			Jackson2JsonMessageConverter jsonMessageConverter = (Jackson2JsonMessageConverter)deleatingMsgConverter.getDelegates()
					.get(RabbitMQConfig.contentTypeJSON);
	
			// Object를 JSON으로 변환하여 Message 객체를 생성합니다.
			Message message = jsonMessageConverter.toMessage(msg, new MessageProperties());
	
			rabbitTemplate.send(exchange, routingKey, message);
		}
	}
	
}
