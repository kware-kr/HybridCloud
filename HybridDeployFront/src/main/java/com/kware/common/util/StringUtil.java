package com.kware.common.util;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtil {

	final static SimpleDateFormat defaultformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
	final static DateTimeFormatter datetimeformatter0 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	final static DateTimeFormatter datetimeformatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	public static String getDateStringFromLocalDateTime(LocalDateTime datetime, int formant_number) {
		String formattedDate = null;
		if(formant_number == 0) {
			formattedDate = datetime.format(datetimeformatter0);
		}else {
			formattedDate = datetime.format(datetimeformatter1);
		}
		return formattedDate;
	}

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
			Date date = defaultformatter.parse(dateStr);
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
     * 문자열을 기본 포맷으로 Timestamp로 변환합니다.
     *
     * @param dateString 변환할 날짜 문자열
     * @return 변환된 Timestamp 객체
     * @throws ParseException 날짜 문자열 포맷 오류 시
     */
    public static Timestamp getTimestamp(String dateString)  {
    	if(dateString == null)
    		return null;
    	
    	Date date = null;
		try {
			date = defaultformatter.parse(dateString);
		} catch (ParseException e) {
			log.error("Timestamp Error {}", dateString, e);
		}
        return new Timestamp(date.getTime());
    }

    /**
     * 문자열을 지정된 포맷으로 Timestamp로 변환합니다.
     *
     * @param dateString 변환할 날짜 문자열
     * @param format 날짜 포맷
     * @return 변환된 Timestamp 객체
     * @throws ParseException 날짜 문자열 포맷 오류 시
     */
    public static Timestamp getTimestamp(SimpleDateFormat formatter, String dateString)  {
    	if(dateString == null)
    		return null;
    	
        Date date = null;
		try {
			date = formatter.parse(dateString);
		} catch (ParseException e) {
			log.error("Timestamp Error {}", dateString, e);
		}
        return new Timestamp(date.getTime());
    }
    
	
	private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
			.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"))  // 2023-08-21 15:30:00
			.appendOptional(DateTimeFormatter.ISO_DATE_TIME)           // 2023-08-21T15:30:00
			.appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))  // 2023-08-21 15:30:00
	        .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)           // 2023-08-21T15:30:00
	        .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE)                // 2023-08-21
	        .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))  // 2023/08/21 15:30:00
	        .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))  // 08/21/2023 15:30:00
	        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
	        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
	        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
	        .toFormatter(Locale.ENGLISH);
	
	public static LocalDateTime parseFlexibleLocalDateTime(String dateString) {
        try {
            return LocalDateTime.parse(dateString, FLEXIBLE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date format not supported: " + dateString, e);
        }
    }
	
	public static Date parseFlexibleDate(String dateString) {
        try {
        	LocalDateTime localDateTime = LocalDateTime.parse(dateString, FLEXIBLE_FORMATTER);
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
            return Date.from(zonedDateTime.toInstant());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date format not supported: " + dateString, e);
        }
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