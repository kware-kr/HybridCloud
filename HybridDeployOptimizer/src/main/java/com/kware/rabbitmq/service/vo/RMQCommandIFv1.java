package com.kware.rabbitmq.service.vo;

import java.util.Base64;

import com.kware.common.util.JSONUtil;

import lombok.Getter;
import lombok.Setter;

/**
 * command interface v1
 */
@Getter
@Setter
public class RMQCommandIFv1 {
    private Header header;
    private Body body;

    // Header Class
    @Getter
    @Setter
    public static class Header {
        private String msg_id;
        private String msg_type; // 명령 
        private String msg_kind;
        private String res_id;
        private int res_code;
        private String res_message;
        private String timestamp;
        private String location;
    }

    // Body Class
    @Getter
    @Setter
    public static class Body {
        private String contents;
        private String encoding; //base64만 지원, 없으면 원문 그대로

        // Decoding Base64 Contents
        /**
         * 내부 컨텐츠를 변경하디 않고 decode return
         * @return
         */
        public String getDecodedContents() {
            if ("base64".equalsIgnoreCase(encoding)) {
                return new String(Base64.getDecoder().decode(contents));
            }
            return contents;
        }
        
        // 현재 내용을 encoding에 맞게 Decoding
        public void setDecodedContents(String contents) {
            if ("base64".equalsIgnoreCase(this.encoding)) {
                this.contents = new String(Base64.getDecoder().decode(contents));
                this.encoding = null;
            }
        }
        
        // Encoding Contents to Base64
        public void encodeContents() {
            if ("base64".equalsIgnoreCase(encoding)) {
                this.contents = Base64.getEncoder().encodeToString(contents.getBytes());
            }
        }
    }
    
 // Method to Convert to JSON
    public String toJson() throws Exception {
       /*
    	ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(this);
        */
        return JSONUtil.getJsonstringFromObject(this);
    }

    // Method to Create Object from JSON
    public static RMQCommandIFv1 fromJson(String json) throws Exception {
    	/*
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CommandInterface.class);
        */
    	return JSONUtil.fromJson(json, RMQCommandIFv1.class );
    }
}
