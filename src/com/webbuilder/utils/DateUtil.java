package com.webbuilder.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	public static String formatDate(Date date, String format) {
		if (date == null)
			return "";
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(date);
	}

	public static String getDateString(Date date) {
		return formatDate(date, "yyyy-MM-dd");
	}

	public static String fixTimestamp(String str) {
		if (str.indexOf(':') == -1)
			return qualify(str) + " 00:00:00";
		else {
			int i = str.indexOf(' ');
			return qualify(str.substring(0, i)) + str.substring(i);
		}
	}

	private static String qualify(String dateStr) {
		if (dateStr.length() == 10)
			return dateStr;
		String[] sec = StringUtil.split(dateStr, "-");
		if (sec.length == 3) {
			StringBuilder buf = new StringBuilder(10);
			buf.append(sec[0]);
			buf.append("-");
			if (sec[1].length() == 1)
				buf.append("0");
			buf.append(sec[1]);
			buf.append("-");
			if (sec[2].length() == 1)
				buf.append("0");
			buf.append(sec[2]);
			return buf.toString();
		} else
			return dateStr;
	}

	public static String fixTime(String str) {
		if (str.indexOf(':') == -1)
			return "00:00:00";
		int b = str.indexOf(' '), e = str.indexOf('.');
		if (b == -1)
			b = 0;
		if (e == -1)
			e = str.length();
		return str.substring(b, e);
	}

	public static String getHours(long milliSecs) {
		long h = milliSecs / 3600000, hm = milliSecs % 3600000;
		long m = hm / 60000, mm = hm % 60000;
		long s = mm / 1000, sm = mm % 1000;
		return StringUtil.concat(Long.toString(h), ":", Long.toString(m), ":",
				Long.toString(s), ".", Long.toString(sm));
	}

	public static int daysInMonth(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	public static int dayOfMonth(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.DAY_OF_MONTH);
	}

	public static int yearOf(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.YEAR);
	}

	public static int dayOfYear(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.DAY_OF_YEAR);
	}

	public static int dayOfWeek(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	public static String toString(Date date) {
		if (date == null)
			return "";
		Timestamp t = new Timestamp(date.getTime());
		return t.toString();
	}

	public static Date incYear(Date date, int years) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.YEAR, years);
		return cal.getTime();
	}

	public static Date incMonth(Date date, int months) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MONTH, months);
		return cal.getTime();
	}

	public static int hourOfDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.HOUR_OF_DAY);
	}

	public static Date incDay(Date date, long days) {
		return new Date(date.getTime() + 86400000 * days);
	}

	public static Date incSecond(Date date, long seconds) {
		return new Date(date.getTime() + 1000 * seconds);
	}
}
