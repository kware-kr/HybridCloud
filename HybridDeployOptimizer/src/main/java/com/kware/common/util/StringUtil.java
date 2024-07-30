package com.kware.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
		try {
			Date date = formatter.parse(dateStr);
			timestamp = date.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return timestamp;
	}

}