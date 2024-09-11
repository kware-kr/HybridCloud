package com.kware.common.config.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsonIgnoreDynamicSerializer extends JsonSerializer<String> {
	
	private static boolean isIgnorerDynamic = false; // 전역 설정

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    	if(!isIgnorerDynamic) {
    		gen.writeString(value);
    	}else {
    		//gen.writeString("");
    		gen.writeNull();
    	}
    }
    
 // 전역 설정을 업데이트하는 메서드
    public static void setIgnoreDynamic(boolean isIgnore) {
    	isIgnorerDynamic = isIgnore;
    }
}