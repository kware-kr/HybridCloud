package com.kware.rabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enable", havingValue = "true")
public class RabbitMQConfig {

	public static String contentTypeJSON   = "application/json";
	public static String contentTypeYAML   = "application/x-yaml";
	public static String contentTypeString = "text/plain";
	
	
	@Value("${spring.rabbitmq.exchange.policy.name}")
	private String policy_exchange;
	
	
	@Value("${spring.rabbitmq.queue.policy_req.name}")
	private String policy_req_queue;
	
	@Value("${spring.rabbitmq.queue.policy_res.name}")
	private String policy_res_queue;
	
	
	@Value("${spring.rabbitmq.routing.policy_req.key}")
	private String policy_req_key;
	
	@Value("${spring.rabbitmq.routing.policy_res.key}")
	private String policy_res_key;
	
	
	
	@Bean
	TopicExchange policy_exchange() {
		return new TopicExchange(this.policy_exchange);
	}
	
	@Bean
	Queue policy_req_queue() {
        return new Queue(this.policy_req_queue, true);
    }
	
	@Bean
	Queue policy_res_queue() {
        return new Queue(this.policy_res_queue, true);
    }
	
	
	@Bean
    public Binding policy_req_binding() {
        return BindingBuilder
        		.bind(this.policy_req_queue())
        		.to(this.policy_exchange())
        		.with(this.policy_req_key);
    }
	
	@Bean
    public Binding policy_res_binding() {
		return BindingBuilder
        		.bind(this.policy_res_queue())
        		.to(this.policy_exchange())
        		.with(this.policy_res_key);
    }

	@Bean("policy_message_converter")  
    public MessageConverter messageConverter() {
        ContentTypeDelegatingMessageConverter converter = new ContentTypeDelegatingMessageConverter();
        converter.addDelegate(RabbitMQConfig.contentTypeJSON, new Jackson2JsonMessageConverter());
        converter.addDelegate(RabbitMQConfig.contentTypeYAML, new YamlMessageConverter());
        return converter;
    }

	/*
	@Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
*/
}