package com.kware.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
/**
*    일반문자열 유틸.
*
* @author someone
* @version 1.0.0
*/
public class JSONUtil {
    /**
     * 로그 출력.
     */
    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger(JSONUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final ObjectMapper jsonWriter = new ObjectMapper();
    {   //ObjectMapper의 속성이 변경되므로, 별도로 하나 만듬
    	jsonWriter.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }
    
    private static final ObjectMapper notNullMapper = new ObjectMapper();
    {
    	//null 값인 필드들이 JSON 출력에서 제외됩니다.
    	notNullMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    	//빈 객체(필드가 없는 객체 또는 모든 필드가 null인 객체)를 직렬화할 때 오류가 발생하지 않고, {}로 표현됩니다
    	notNullMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
    
    
    public static String getJsonstringFromObject(Object obj) throws JsonProcessingException {
        return jsonWriter.writeValueAsString(obj);
    }
    
   // Generic Method to Create Object from JSON
    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
    
    //Map을 임의의 클래스 객체로 변환하는 static 제네릭 메서드
    public static <T> T convertMapToObject(Map<String, Object> map, Class<T> clazz) {
        return objectMapper.convertValue(map, clazz);
    }

    
    /**
     * Map을 JSONObject로 변환한다.
     *
     * @param map Map<String, Object>.
     * @return String.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject getJsonObjectFromMap( Map<String, Object> map ) {
    	JSONObject json = new JSONObject(map);
        /*for( Map.Entry<String, Object> entry : map.entrySet() ) {
            String key = entry.getKey();
            Object value = entry.getValue();
            json.put(key, value);
        }
        */
        return json;
    }
    
    /**
     * List<Map>을 JSONArray로 변환한다.
     *
     * @param list List<Map<String, Object>>.
     * @return JSONArray.
     */
    @SuppressWarnings("unchecked")
    public static JSONArray getJsonArrayFromList( List<Map<String, Object>> list ) {
        JSONArray jsonArray = new JSONArray();
        for( Map<String, Object> map : list ) {
            jsonArray.add( getJsonStringFromMap( map ) );
        }
        
        return jsonArray;
    }
    
    /**
     * List<Map>을 jsonString으로 변환한다.
     *
     * @param list List<Map<String, Object>>.
     * @return String.
     */
    @SuppressWarnings("unchecked")
    public static String getJsonStringFromList( List<Map<String, Object>> list ) {
        JSONArray jsonArray = new JSONArray();
        for( Map<String, Object> map : list ) {
            jsonArray.add( getJsonStringFromMap( map ) );
        }
        
        return jsonArray.toJSONString();
    }
    /**
     * JsonObject를 Map<String, String>으로 변환한다.
     *
     * @param jsonObj JSONObject.
     * @return String.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMapFromJsonObject( JSONObject jsonObj ) {
        Map<String, Object> map = null;
        
        try {
            map = objectMapper.readValue(jsonObj.toJSONString(), Map.class) ;
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
    /**
     * JsonArray를 List<Map<String, String>>으로 변환한다.
     *
     * @param jsonArray JSONArray.
     * @return List<Map<String, Object>>.
     */
    public static List<Map<String, Object>> getListMapFromJsonArray( JSONArray jsonArray ) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        
        if( jsonArray != null )
        {
            int jsonSize = jsonArray.size();
            for( int i = 0; i < jsonSize; i++ )
            {
                Map<String, Object> map = JSONUtil.getMapFromJsonObject( ( JSONObject ) jsonArray.get(i) );
                list.add( map );
            }
        }
        
        return list;
    }
    
    /**
     * JsonString를 Map<String, String>으로 변환한다.
     *
     * @param jsonstring String 
     * @return String.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMapFromJsonString( String jsonstrng ) {
        Map<String, Object> map = null;
        
        try {
            map = objectMapper.readValue(jsonstrng, Map.class) ;
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
    
    /**
     * JsonString를 Map<String, String>으로 변환한다.
     *
     * @param jsonstring String 
     * @return String.
     */
    @SuppressWarnings("unchecked")
    public static String getJsonStringFromMap( Map map ) {
        JSONObject jsonObject = new JSONObject(map);

        // JSONObject를 JSON 문자열로 변환
        return jsonObject.toJSONString();
    }
    
    public static String getJsonstringFromObject(Object object, Set<String> fieldsToExclude) throws JsonProcessingException {
        // 필터 설정
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("dynamicFilter",
                SimpleBeanPropertyFilter.serializeAllExcept(fieldsToExclude));

        notNullMapper.setFilterProvider(filterProvider);
        notNullMapper.addMixIn(Object.class, DynamicFilterMixIn.class);

        return notNullMapper.writeValueAsString(object);
    }

    // 믹스인 어노테이션 설정
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.annotation.JsonFilter("dynamicFilter")
    public static class DynamicFilterMixIn {
    }

} 