package com.webbuilder.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webbuilder.common.Var;

public class SysUtil {
	private static long currentId = 0;
	private static char serverId;
	private static Object idLock = new Object();

	public static String getId() {
		long id;
		synchronized (idLock) {
			if (currentId == 0) {
				currentId = (new Date()).getTime() * 10000;
				try {
					serverId = Var.get("server.serverId").charAt(0);
				} catch (Throwable e) {
					serverId = '2';
				}
			}
			id = currentId++;
		}
		return numToString(id);
	}

	private static char intToChar(int val) {
		if (val < 10)
			return (char) (val + 48);
		else
			return (char) (val + 55);
	}

	private static String numToString(long num) {
		char[] buf = new char[12];
		int charPos = 12;
		long val;

		buf[0] = serverId;
		while ((val = num / 36) > 0) {
			buf[--charPos] = intToChar((int) (num % 36));
			num = val;
		}
		buf[--charPos] = intToChar((int) (num % 36));
		return new String(buf);
	}

	public static String getShortError(Throwable e) {
		Throwable cause = e, c = e;

		while (c != null) {
			cause = c;
			c = c.getCause();
		}
		String message = cause.getMessage();
		if (StringUtil.isEmpty(message))
			message = cause.toString();
		return StringUtil.toLine(message);
	}

	public static void executeMethod(String classMethodName,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		int pos = classMethodName.lastIndexOf('.');
		String className, methodName;

		if (pos == -1) {
			className = "";
			methodName = classMethodName;
		} else {
			className = classMethodName.substring(0, pos);
			methodName = classMethodName.substring(pos + 1);
		}
		Class<?> cls = Class.forName(className);
		cls.getMethod(methodName, HttpServletRequest.class,
				HttpServletResponse.class).invoke(cls, request, response);
	}

	public static void executeMethod(String classMethodName) throws Exception {
		if (StringUtil.isEmpty(classMethodName))
			return;
		int pos = classMethodName.lastIndexOf('.');
		String className, methodName;

		if (pos == -1) {
			className = "";
			methodName = classMethodName;
		} else {
			className = classMethodName.substring(0, pos);
			methodName = classMethodName.substring(pos + 1);
		}
		Class<?> cls = Class.forName(className);
		cls.getMethod(methodName).invoke(cls);
	}

	public static int isToOs(InputStream is, OutputStream os) throws Exception {
		byte buf[] = new byte[8192];
		int len, size = 0;

		while ((len = is.read(buf)) > 0) {
			os.write(buf, 0, len);
			size += len;
		}
		return size;
	}

	public static String readString(Reader reader) throws Exception {
		char buf[] = new char[8192];
		StringBuilder sb = new StringBuilder();
		int len;

		while ((len = reader.read(buf)) > 0) {
			sb.append(buf, 0, len);
		}
		return sb.toString();
	}

	public static void closeInputStream(InputStream inputStream) {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (Throwable e) {
			}
		}
	}

	public static void error(String msg) throws Exception {
		throw new Exception(msg);
	}
}
