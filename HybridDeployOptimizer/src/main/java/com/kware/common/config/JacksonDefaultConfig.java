package com.kware.common.config;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

@Configuration
public class JacksonDefaultConfig {

    private static final String dateFormat = "yyyy-MM-dd";
    private static final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
        	builder.timeZone("Asia/Seoul");
            builder.simpleDateFormat(dateTimeFormat);
            
            builder.serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat))
                              , new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat))
//                              , new TimestampSerializer(DateTimeFormatter.ofPattern(dateTimeFormat))
            );
            
            builder.deserializers( new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat))
            					 , new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat))           
//                                 , new TimestampDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat))
            );
            
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
    
 /*   
    //전역으로 처리할려고 했는데 잘 안되네, 그래서 각 timestamp에서 처리함
    public class TimestampSerializer extends StdSerializer<Timestamp> {

        private final DateTimeFormatter formatter;

        public TimestampSerializer(DateTimeFormatter formatter) {
            super(Timestamp.class);
            this.formatter = formatter;
        }

        @Override
        public void serialize(Timestamp value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            String formattedDateTime = value.toLocalDateTime().format(formatter);
            gen.writeString(formattedDateTime);
        }
    }
    
    public class TimestampDeserializer extends StdDeserializer<Timestamp> {

        private final DateTimeFormatter formatter;

        public TimestampDeserializer(DateTimeFormatter formatter) {
            super(Timestamp.class);
            this.formatter = formatter;
        }

        @Override
        public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String date = p.getText();
            LocalDateTime localDateTime = LocalDateTime.parse(date, formatter);
            return Timestamp.valueOf(localDateTime);
        }
    }
    */
}