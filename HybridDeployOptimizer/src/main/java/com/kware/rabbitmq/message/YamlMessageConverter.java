package com.kware.rabbitmq.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.io.IOException;

public class YamlMessageConverter implements MessageConverter {

    private final ObjectMapper objectMapper;

    public YamlMessageConverter() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public Message toMessage(Object object, org.springframework.amqp.core.MessageProperties messageProperties) throws MessageConversionException {
        try {
            String yamlString = objectMapper.writeValueAsString(object);
            return new Message(yamlString.getBytes(), messageProperties);
        } catch (IOException e) {
            throw new MessageConversionException("Error converting object to YAML", e);
        }
    }

    @Override
    public Object fromMessage(org.springframework.amqp.core.Message message) throws MessageConversionException {
        try {
            return objectMapper.readValue(message.getBody(), Object.class);
        } catch (IOException e) {
            throw new MessageConversionException("Error converting YAML to object", e);
        }
    }
}