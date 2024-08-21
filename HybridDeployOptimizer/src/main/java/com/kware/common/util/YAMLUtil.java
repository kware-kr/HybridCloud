package com.kware.common.util;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
@Slf4j
public class YAMLUtil {

	final static YAMLFactory yamlFactory = new YAMLFactory();
	final static ObjectMapper mapper = new ObjectMapper(yamlFactory);
    static {
    	//mapper.setSerializationInclusion(Include.NON_NULL);
    	yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    }
    
   
    public static <T> T read(File _file, Class<T> _valueType) {
        try {
            return mapper.readValue(_file, _valueType);
        } catch (IOException e) {
            log.error("YAML getRead Error", e);
            return null; 
        }
    }
    
    public static <T> T read(String _string, Class<T> _valueType) {
        try {
            return mapper.readValue(_string, _valueType);
        } catch (IOException e) {
            log.error("YAML getRead Error", e);
            return null; 
        }
    }
    
    //동적 필터 적용
    public static <T> String writeString(T value, String filterName, SimpleBeanPropertyFilter filter) {
        try {
            FilterProvider filters = new SimpleFilterProvider().addFilter(filterName, filter);
            return mapper.writer(filters).writeValueAsString(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static <T> String writeString(T _value) {
        try {
            return mapper.writeValueAsString(_value);
        } catch (IOException e) {
            log.error("YAML getString Error", e);
            return null; 
        }
    }
    
    public static <T> String writeString(T _value, boolean base64encoding) {
    	if(_value == null)
    		return null;
    	
        String rtnstring = null;
        
        if(base64encoding == true)
    		rtnstring = StringUtil.encodebase64(writeString(_value));
    	else rtnstring = writeString(_value);
        
    	return rtnstring;
    }
    
    public static <T> void writeFile(File _file, T _value) {
        try {
            mapper.writeValue(_file, _value);
        } catch (IOException e) {
            log.error("YAML getString Error", e);
        }
    }
    
    //yaml string을 jsonString으로 변환하는 함수
    public static String convertYamlToJson(String yamlString) throws IOException {
        // YAML 문자열을 ObjectMapper를 사용하여 JSON 문자열로 변환
        JSONObject obj = mapper.readValue(yamlString, JSONObject.class);
        
        ObjectMapper jsonWriter = new ObjectMapper();
        
        String rtnstring = jsonWriter.writeValueAsString(obj); 
        obj.clear();
        return rtnstring;
    }
    
    public static String convertYamlToJson(String yamlString, boolean base64encoding) throws IOException {
    	if(yamlString == null)
    		return null;
    	
    	String rtnstring = null;
    	if(base64encoding == true)
    		rtnstring = StringUtil.encodebase64(convertYamlToJson(yamlString));
    	else rtnstring = convertYamlToJson(yamlString);
    	return rtnstring;
    }
    
    
} 