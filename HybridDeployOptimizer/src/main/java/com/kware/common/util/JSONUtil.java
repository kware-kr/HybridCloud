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
    
    public static String getJsonstringFromObject(Object obj) throws JsonProcessingException {
    	ObjectMapper jsonWriter = new ObjectMapper();
    	jsonWriter.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return jsonWriter.writeValueAsString(obj);
    }
    /**
     * Map을 jsonString으로 변환한다.
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
     * List<Map>을 json으로 변환한다.
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
            map = new ObjectMapper().readValue(jsonObj.toJSONString(), Map.class) ;
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
            map = new ObjectMapper().readValue(jsonstrng, Map.class) ;
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
    
    public static String toJsonString(Object object, Set<String> fieldsToExclude) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 필터 설정
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("dynamicFilter",
                SimpleBeanPropertyFilter.serializeAllExcept(fieldsToExclude));

        mapper.setFilterProvider(filterProvider);
        mapper.addMixIn(Object.class, DynamicFilterMixIn.class);

        return mapper.writeValueAsString(object);
    }

    // 믹스인 어노테이션 설정
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.annotation.JsonFilter("dynamicFilter")
    public static class DynamicFilterMixIn {
    }

} 