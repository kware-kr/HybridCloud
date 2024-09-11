package com.kware.common.config.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HumanReadableSizeSerializer extends JsonSerializer<Long> {
	
	private static boolean humanReadable = false; // 전역 설정

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    	if(humanReadable) {
    		gen.writeString(convertToHumanReadable(value));
    	}else {
    		gen.writeNumber(value);
    	}
    }

    private String convertToHumanReadable(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
    
 // 전역 설정을 업데이트하는 메서드
    public static void setHumanReadable(boolean readable) {
        humanReadable = readable;
    }
}