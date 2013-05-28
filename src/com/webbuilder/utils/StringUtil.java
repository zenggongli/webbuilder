package com.webbuilder.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

import com.webbuilder.common.Str;
import com.webbuilder.common.Var;

public class StringUtil {
	private static final String HexCharSet = "0123456789ABCDEF";

	public static String[] sort(List<String> list) {
		String[] sl = list.toArray(new String[list.size()]);
		return StringUtil.sort(sl);
	}

	public static String[] sort(String[] list) {
		Arrays.sort(list, new Comparator<String>() {
			Collator collator = Collator.getInstance();

			public int compare(String s1, String s2) {
				CollationKey key1 = collator.getCollationKey(optString(s1)
						.toLowerCase());
				CollationKey key2 = collator.getCollationKey(optString(s2)
						.toLowerCase());
				return key1.compareTo(key2);
			}
		});
		return list;
	}

	public static String duplicate(String text, int count) {
		StringBuilder buf = new StringBuilder();
		int i;

		for (i = 0; i < count; i++)
			buf.append(text);
		return buf.toString();
	}

	public static String[] getList(String... args) {
		String r[] = new String[args.length];
		int i = 0;

		for (String s : args)
			r[i++] = s;
		return r;
	}

	public static String[] split(String string, String separator) {
		String[] result;
		if (isEmpty(string)) {
			result = new String[1];
			result[0] = "";
			return result;
		}
		result = new String[stringOccur(string, separator) + 1];
		int oldPos = 0, pos = 0, count = 0, len = separator.length();
		while (pos != -1) {
			pos = string.indexOf(separator, oldPos);
			if (pos != -1) {
				result[count++] = string.substring(oldPos, pos);
				pos += len;
				oldPos = pos;
			}
		}
		result[count] = string.substring(oldPos);
		return result;
	}

	public static boolean isSame(String string1, String string2) {
		String s1, s2;

		if (string1 == null)
			s1 = "";
		else
			s1 = string1;
		if (string2 == null)
			s2 = "";
		else
			s2 = string2;
		return s1.equalsIgnoreCase(s2);
	}

	public static boolean isEqual(String string1, String string2) {
		String s1, s2;

		if (string1 == null)
			s1 = "";
		else
			s1 = string1;
		if (string2 == null)
			s2 = "";
		else
			s2 = string2;
		return s1.equals(s2);
	}

	public static String toHTML(String string) {
		return toHTML(string, false, true);
	}

	public static String convertHTML(String string) {
		if (isEmpty(string))
			return "";
		int i, j = string.length();
		StringBuilder out = new StringBuilder();
		char c;

		for (i = 0; i < j; i++) {
			c = string.charAt(i);
			switch (c) {
			case '&':
				out.append("&amp;");
				break;
			case '>':
				out.append("&gt;");
				break;
			case '<':
				out.append("&lt;");
				break;
			default:
				out.append(c);
			}
		}
		return out.toString();
	}

	public static String toHTML(String string, boolean nbspAsEmpty,
			boolean brAsEnter) {
		if (isEmpty(string)) {
			if (nbspAsEmpty)
				return "&nbsp;";
			else
				return "";
		}
		int i, j = string.length();
		StringBuilder out = new StringBuilder();
		char c;

		for (i = 0; i < j; i++) {
			c = string.charAt(i);
			switch (c) {
			case ' ':
				out.append("&nbsp;");
				break;
			case '"':
				out.append("&quot;");
				break;
			case '<':
				out.append("&lt;");
				break;
			case '>':
				out.append("&gt;");
				break;
			case '&':
				out.append("&amp;");
				break;
			case '\n':
				if (brAsEnter)
					out.append("<br>");
				else
					out.append("&nbsp;");
				break;
			case '\r':
				break;
			case '\t':
				out.append("&nbsp;&nbsp;&nbsp;&nbsp;");
				break;
			default:
				out.append(c);
			}
		}
		return out.toString();
	}

	public static boolean across(String[] list1, String[] list2) {
		if (list1 == null || list2 == null)
			return false;
		for (String s1 : list1)
			for (String s2 : list2)
				if (s1.equals(s2))
					return true;
		return false;
	}

	public static int indexOf(String list[], String string) {
		int i, j;

		if (list == null)
			return -1;
		j = list.length;
		for (i = 0; i < j; i++)
			if (list[i].equals(string))
				return i;
		return -1;
	}

	public static String concat(String s, String... more) {
		StringBuilder buf = new StringBuilder(s);
		for (String t : more)
			buf.append(t);
		return buf.toString();
	}

	public static boolean getBool(String value) {
		return isEqual(value, "1") || isSame(value, "true");
	}

	public static int stringOccur(String source, String dest) {
		if (isEmpty(source) || isEmpty(dest))
			return 0;
		int pos = 0, count = 0;
		while (pos != -1) {
			pos = source.indexOf(dest, pos);
			if (pos != -1) {
				pos++;
				count++;
			}
		}
		return count;
	}

	public static boolean isNumeric(String string, boolean decimal) {
		int i, j;
		String ts;
		char ch;

		ts = string.trim();
		if (decimal && stringOccur(string, ".") > 1)
			return false;
		if (ts.length() > 1 && isEqual(ts.substring(0, 1), "-"))
			ts = ts.substring(1);
		j = ts.length();
		if (j == 0)
			return false;
		for (i = 0; i < j; i++) {
			ch = ts.charAt(i);
			if (!(ch >= '0' && ch <= '9') && (!decimal || ch != '.'))
				return false;
		}
		return true;
	}

	public static String toLine(String string) {
		if (string == null)
			return "";
		string = string.trim();
		int i, len = string.length();
		if (len == 0)
			return "";
		StringBuilder buffer = new StringBuilder();
		char c;
		for (i = 0; i < len; i++) {
			c = string.charAt(i);
			switch (c) {
			case '\n':
			case '\r':
			case '\t':
				buffer.append(' ');
				break;
			default:
				buffer.append(c);
			}
		}
		return buffer.toString();
	}

	public static List<Entry<String, ?>> sortMapValue(Map<String, ?> map) {
		List<Entry<String, ?>> list = new ArrayList<Entry<String, ?>>(map
				.entrySet());
		Collections.sort(list, new Comparator<Entry<String, ?>>() {
			Collator collator = Collator.getInstance();

			public int compare(Entry<String, ?> e1, Entry<String, ?> e2) {
				CollationKey key1 = collator.getCollationKey(e1.getValue()
						.toString().toLowerCase());
				CollationKey key2 = collator.getCollationKey(e2.getValue()
						.toString().toLowerCase());
				return key1.compareTo(key2);
			}
		});
		return list;
	}

	public static List<Entry<String, ?>> sortMapKey(Map<String, ?> map) {
		return sortMapKey(map, false);
	}

	public static List<Entry<String, ?>> sortMapKey(Map<String, ?> map,
			boolean keyAsNumber) {
		List<Entry<String, ?>> list = new ArrayList<Entry<String, ?>>(map
				.entrySet());
		final boolean keyAsNum = keyAsNumber;
		Collections.sort(list, new Comparator<Entry<String, ?>>() {
			Collator collator = Collator.getInstance();

			public int compare(Entry<String, ?> e1, Entry<String, ?> e2) {
				if (keyAsNum)
					return Integer.parseInt(e1.getKey())
							- Integer.parseInt(e2.getKey());
				else {
					CollationKey key1 = collator.getCollationKey(e1.getKey()
							.toLowerCase());
					CollationKey key2 = collator.getCollationKey(e2.getKey()
							.toLowerCase());
					return key1.compareTo(key2);
				}
			}
		});
		return list;
	}

	public static String getNamePart(String string) {
		if (string == null)
			return "";
		int index = string.indexOf('=');

		if (index == -1)
			return string;
		else
			return string.substring(0, index);
	}

	public static String getValuePart(String string) {
		if (string == null)
			return "";
		int index = string.indexOf('=');

		if (index == -1)
			return "";
		else
			return string.substring(index + 1);
	}

	public static String replaceParameters(HttpServletRequest request,
			String text) throws Exception {
		int start = 0, startPos = text.indexOf("{#", start), endPos = text
				.indexOf("#}", startPos + 2);
		if (startPos != -1 && endPos != -1) {
			String prefix, paramName, paramValue;
			StringBuilder buf = new StringBuilder();

			while (startPos != -1 && endPos != -1) {
				paramName = text.substring(startPos + 2, endPos);
				prefix = substring(paramName, 0, 4);
				if (prefix.equals("Var.")) {
					paramValue = Var.get(paramName.substring(4));
					if (paramValue == null)
						paramValue = "";
				} else if (prefix.equals("Str."))
					paramValue = Str.format(request, paramName.substring(4));
				else
					paramValue = WebUtil.fetch(request, paramName);
				buf.append(text.substring(start, startPos));
				buf.append(paramValue);
				start = endPos + 2;
				startPos = text.indexOf("{#", start);
				endPos = text.indexOf("#}", startPos + 2);
			}
			buf.append(text.substring(start));
			return buf.toString();
		} else
			return text;
	}

	public static String replace(String string, String oldString,
			String newString) {
		return innerReplace(string, oldString, newString, true);
	}

	public static String replaceFirst(String string, String oldString,
			String newString) {
		return innerReplace(string, oldString, newString, false);
	}

	public static String substring(String string, int pos, int len) {
		if (string == null || string.length() < len - pos)
			return "";
		return string.substring(pos, pos + len);
	}

	public static String joinArray(String list[], String spliter) {
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;

		for (String s : list) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(spliter);
			buf.append(s);
		}
		return buf.toString();
	}

	public static String joinList(Iterable<String> list, String spliter) {
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;

		for (String s : list) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(spliter);
			buf.append(s);
		}
		return buf.toString();
	}

	public static String getUtfString(InputStream stream) throws Exception {
		return getString(stream, "utf-8");
	}

	public static String getString(InputStream stream) throws Exception {
		return getString(stream, Var.get("server.charset"));
	}

	public static String getString(InputStream stream, String charset)
			throws Exception {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			SysUtil.isToOs(stream, os);
			if (StringUtil.isEmpty(charset))
				return new String(os.toByteArray());
			else
				return new String(os.toByteArray(), charset);
		} finally {
			stream.close();
		}
	}

	private static String innerReplace(String string, String oldString,
			String newString, boolean isAll) {
		if (string == null)
			return "";
		int index = string.indexOf(oldString);
		if (index == -1)
			return string;
		int start = 0, len = oldString.length();
		if (len == 0)
			return string;
		StringBuilder buffer = new StringBuilder(string.length() + len);
		do {
			buffer.append(string.substring(start, index));
			buffer.append(newString);
			start = index + len;
			if (!isAll)
				break;
			index = string.indexOf(oldString, start);
		} while (index != -1);
		buffer.append(string.substring(start));
		return buffer.toString();
	}

	public static String quote(String string) {
		if (string == null)
			return "\"\"";
		int len = string.length();
		if (len == 0)
			return "\"\"";
		char b, c = 0;
		int i;
		String t;
		StringBuilder sb = new StringBuilder(len + 4);

		sb.append('"');
		for (i = 0; i < len; i++) {
			b = c;
			c = string.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				sb.append('\\');
				sb.append(c);
				break;
			case '/':
				if (b == '<')
					sb.append('\\');
				sb.append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case 0x0b:
				sb.append("\\u000b");
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
						|| (c >= '\u2000' && c < '\u2100')) {
					t = "000" + Integer.toHexString(c);
					sb.append("\\u");
					sb.append(t.substring(t.length() - 4));
				} else
					sb.append(c);
			}
		}
		sb.append('"');
		return sb.toString();
	}

	public static String[] merge(String[] list1, String[] list2) {
		int l = list1.length, i, j = l + list2.length;
		String[] list = new String[j];
		for (i = 0; i < j; i++) {
			if (i < l)
				list[i] = list1[i];
			else
				list[i] = list2[i - l];
		}
		return list;
	}

	public static String optString(String s) {
		if (s == null)
			return "";
		else
			return s;
	}

	public static boolean isEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static String byteToHex(byte[] bs) {
		StringBuilder buf = new StringBuilder(bs.length * 2);
		String s;
		for (byte b : bs) {
			s = Integer.toHexString(b & 0XFF);
			if (s.length() == 1)
				buf.append('0');
			buf.append(s);
		}
		return buf.toString().toUpperCase();
	}

	public static byte[] hexToByte(String s) {
		int i, j = s.length() / 2, k;
		char[] b = s.toCharArray();
		byte[] d = new byte[j];

		for (i = 0; i < j; i++) {
			k = i * 2;
			d[i] = (byte) (HexCharSet.indexOf(b[k]) << 4 | HexCharSet
					.indexOf(b[k + 1]));
		}
		return d;
	}

	public static String encodeBase64(InputStream is) throws Exception {
		OutputStream eos = null;
		ByteArrayOutputStream bos1, bos2;

		try {
			bos1 = new ByteArrayOutputStream();
			bos2 = new ByteArrayOutputStream();
			SysUtil.isToOs(is, bos1);
			eos = MimeUtility.encode(bos2, "base64");
			eos.write(bos1.toByteArray());
		} finally {
			is.close();
			if (eos != null)
				eos.close();
		}
		return new String(bos2.toByteArray());
	}

	public static byte[] decodeBase64(String data) throws Exception {
		ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		InputStream bis;

		bis = MimeUtility.decode(is, "base64");
		try {
			SysUtil.isToOs(bis, os);
		} finally {
			bis.close();
		}
		return os.toByteArray();
	}

	public static String encode(Object obj) throws Exception {
		if (obj == null)
			return "null";
		else {
			if (obj instanceof InputStream)
				return quote(encodeBase64((InputStream) obj));
			if (obj instanceof Number || obj instanceof Boolean)
				return obj.toString();
			else
				return quote(obj.toString());
		}
	}
}