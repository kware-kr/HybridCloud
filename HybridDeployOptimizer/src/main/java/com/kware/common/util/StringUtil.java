package com.kware.common.util;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

public class StringUtil {

	final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

	public static String getToday() {
		String timestamp;
		//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		timestamp = formatter.format(LocalDateTime.now());

		return timestamp;
	}
	
	public static String getToday(SimpleDateFormat formatter) {
		String timestamp;
		timestamp = formatter.format(LocalDateTime.now());
		return timestamp;
	}

	public static long getMilliseconds(String dateStr) {
		long timestamp = 0;
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = formatter.parse(dateStr);
			timestamp = date.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return timestamp;
	}

	public static long getMilliseconds(SimpleDateFormat formatter, String dateStr) {
		long timestamp = 0;
		if(dateStr != null){
			try {
				Date date = formatter.parse(dateStr);
				timestamp = date.getTime();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return timestamp;
	}
	
	
	/**
	 *   
	 * @param input String
	 * @param charset
	 * StandardCharsets.UTF_8);
       Charset.forName("ISO-8859-1"));
	 * @return
	 */
	// 주어진 인코딩을 사용하는 Base64 인코딩 함수
    public static String encodebase64(String input, Charset charset) {
        return Base64.getEncoder().encodeToString(input.getBytes(charset));
    }

    // 주어진 인코딩을 사용하는 Base64 디코딩 함수
    public static String decodebase64(String input, Charset charset) {
        byte[] decodedBytes = Base64.getDecoder().decode(input);
        return new String(decodedBytes, charset);
    }
	
 // 기본 문자 인코딩을 사용하는 Base64 인코딩 함수
    public static String encodebase64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    // 기본 문자 인코딩을 사용하는 Base64 디코딩 함수
    public static String decodebase64(String input) {
        byte[] decodedBytes = Base64.getDecoder().decode(input);
        return new String(decodedBytes);
    }

}