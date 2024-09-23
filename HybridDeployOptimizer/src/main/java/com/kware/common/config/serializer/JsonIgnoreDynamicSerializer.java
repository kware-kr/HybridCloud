package com.kware.common.config.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsonIgnoreDynamicSerializer extends JsonSerializer<Object> {

    private static boolean isIgnorerDynamic = false;

 

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (isIgnorerDynamic) {
            gen.writeNull();
        } else {
            if (value instanceof String) {
                gen.writeString((String) value);
            } else {
                gen.writeObject(value);
            }
        }
    }

    // 설정을 업데이트하는 메서드
    public static void setIgnoreDynamic(boolean isIgnore) {
    	JsonIgnoreDynamicSerializer.isIgnorerDynamic = isIgnore;
    }
}
