package com.webbuilder.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Parser;
import com.webbuilder.common.Str;
import com.webbuilder.common.Var;
import com.webbuilder.common.XwlBuffer;
import com.webbuilder.common.XwlData;
import com.webbuilder.tool.PageInfo;
import com.webbuilder.tool.QueueWriter;

public class WebUtil {
	public static String request(String url, JSONObject params)
			throws Exception {
		return new String(requestData(url, params), "utf-8");
	}

	public static byte[] requestData(String url, JSONObject params)
			throws Exception {
		HttpURLConnection conn = (HttpURLConnection) (new URL(url))
				.openConnection();
		try {
			byte[] data = getParamsText(params).getBytes("utf-8");
			int timeout = Var.getInt("webbuilder.session.submitTimeout");
			conn.setConnectTimeout(timeout);
			conn.setReadTimeout(timeout);
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded; charset=utf-8");
			conn.setRequestProperty("Content-Length", Integer
					.toString(data.length));
			OutputStream os = conn.getOutputStream();
			try {
				os.write(data);
				os.flush();
			} finally {
				os.close();
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			InputStream is = conn.getInputStream();
			try {
				SysUtil.isToOs(is, bos);
			} finally {
				is.close();
			}
			return bos.toByteArray();
		} finally {
			conn.disconnect();
		}
	}

	private static String getParamsText(JSONObject jo) throws Exception {
		if (jo == null)
			return "";
		StringBuilder sb = new StringBuilder();
		Iterator<?> names = jo.keys();
		String name;
		boolean isFirst = true;

		while (names.hasNext()) {
			name = (String) names.next();
			if (isFirst)
				isFirst = false;
			else
				sb.append("&");
			sb.append(name);
			sb.append("=");
			sb.append(URLEncoder.encode(JsonUtil.optString(jo, name), "utf-8"));
		}
		return sb.toString();
	}

	public static PageInfo getPage(HttpServletRequest request) throws Exception {
		String sv = WebUtil.fetch(request, "start");
		String lv = WebUtil.fetch(request, "limit");
		int start, limit, limitRecords;
		PageInfo pageInfo = new PageInfo();

		if (StringUtil.isEmpty(sv))
			start = 0;
		else
			start = Integer.parseInt(sv);
		if (StringUtil.isEmpty(lv))
			limit = Integer.MAX_VALUE - start;
		else
			limit = Integer.parseInt(lv);
		pageInfo.start = start;
		pageInfo.end = start + limit - 1;
		request.setAttribute("start", start + 1);
		request.setAttribute("end", pageInfo.end + 1);
		limitRecords = Var.getInt("webbuilder.control.limitRecords");
		if (limitRecords == -1)
			limitRecords = Integer.MAX_VALUE;
		pageInfo.limit = limitRecords;
		pageInfo.count = 0;
		return pageInfo;
	}

	public static int checkPage(PageInfo pageInfo) {
		return checkPage(pageInfo, true, true);
	}

	public static int checkPage(PageInfo pageInfo, boolean paged,
			boolean totalCount) {
		int index = pageInfo.count;
		int result;

		if (index >= pageInfo.limit)
			result = 1;
		else if (paged && index < pageInfo.start)
			result = 2;
		else if (paged && index > pageInfo.end) {
			if (totalCount)
				result = 2;
			else
				result = 1;
		} else
			result = 0;
		if (result != 1)
			pageInfo.count++;
		return result;
	}

	public static void setTotal(StringBuilder buf, PageInfo pageInfo) {
		buf.insert(0, "{total:" + Integer.toString(pageInfo.count));
	}

	public static void setCb(StringBuilder buf, String cb) {
		if (cb != null) {
			buf.insert(0, cb + "(");
			buf.append(");");
		}
	}

	public static String[] getSortInfo(HttpServletRequest request)
			throws Exception {
		String sort = request.getParameter("sort");

		if (StringUtil.isEmpty(sort))
			return null;
		JSONObject jo = new JSONArray(sort).getJSONObject(0);
		String[] result = new String[2];
		result[0] = jo.getString("property");
		result[1] = jo.optString("direction");
		return result;
	}

	public static String encodeFilename(HttpServletRequest request, String name)
			throws Exception {
		String agent = StringUtil.optString(request.getHeader("user-agent"))
				.toLowerCase();
		if (name == null)
			name = "";
		if (agent.indexOf("msie") != -1)
			return StringUtil.concat("filename=\"", encodeString(name), "\"");
		else if (agent.indexOf("opera") != -1)
			return StringUtil.concat("filename*=\"utf-8''", encodeString(name),
					"\"");
		else
			return StringUtil.concat("filename=\"", new String(name
					.getBytes("utf-8"), "ISO-8859-1"), "\"");
	}

	public static String encodeString(String str) throws Exception {
		return StringUtil.replace(URLEncoder.encode(str, "utf-8"), "+", "%20");
	}

	public static String decode(String str) throws Exception {
		if (StringUtil.isEmpty(str))
			return str;
		return new String(str.getBytes("ISO-8859-1"), "utf-8");
	}

	public static void clearUploadFile(HttpServletRequest request, List<?> list) {
		FileItem item;
		for (Object t : list) {
			item = (FileItem) t;
			if (!item.isFormField())
				SysUtil.closeInputStream((InputStream) request
						.getAttribute(item.getFieldName()));
			item.delete();
		}
		String uploadId = (String) request.getAttribute("sys.uploadId");
		if (uploadId != null) {
			HttpSession session = request.getSession(true);
			session.removeAttribute("sys.upread." + uploadId);
			session.removeAttribute("sys.uplen." + uploadId);
		}
	}

	public static List<?> setUploadFile(HttpServletRequest request)
			throws Exception {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		List<?> list;
		String fieldName;
		FileItem item;
		final String uploadId = request.getParameter("__uploadId");

		factory.setSizeThreshold(Var
				.getInt("webbuilder.service.upload.bufferSize"));
		request.setAttribute("sys.uploadId", uploadId);
		if (uploadId != null && uploadId.indexOf('.') == -1) {
			final HttpSession session = request.getSession(true);
			if (session != null) {
				upload.setProgressListener(new ProgressListener() {
					public void update(long read, long length, int id) {
						session.setAttribute("sys.upread." + uploadId, read);
						session.setAttribute("sys.uplen." + uploadId, length);
					}
				});
			}
		}
		upload.setSizeMax(Var.getLong("webbuilder.service.upload.maxSize"));
		list = upload.parseRequest(request);
		if (list == null || list.size() == 0)
			return null;
		try {
			for (Object obj : list) {
				item = (FileItem) obj;
				fieldName = item.getFieldName();
				if (fieldName.indexOf('.') != -1)
					continue;
				if (item.isFormField())
					request.setAttribute(fieldName, item.getString("utf-8"));
				else {
					request.setAttribute(fieldName, item.getInputStream());
					request.setAttribute(fieldName + "__name", FileUtil
							.extractFilename(item.getName()));
					request.setAttribute(fieldName + "__size", item.getSize());
				}
			}
		} catch (Throwable e) {
			WebUtil.clearUploadFile(request, list);
			throw new Exception(e);
		}
		return list;
	}

	public static void showException(Throwable exception,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		StringWriter writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer, true);
		exception.printStackTrace(pwriter);
		pwriter.close();
		String stackMsg = writer.toString();

		if (Var.getBool("server.printError"))
			System.err.println(stackMsg);
		println(request, stackMsg);
		if (!response.isCommitted()) {
			response.reset();
			request.setCharacterEncoding("utf-8");
			response.setContentType("text/html;charset=utf-8");
			String shortError = StringUtil.convertHTML(SysUtil
					.getShortError(exception));
			if (WebUtil.isFormSubmit(request))
				WebUtil.response(response, shortError, false);
			else {
				request.setAttribute("sys.shortError", shortError);
				request.setAttribute("sys.longError", StringUtil
						.toHTML(stackMsg));
				response
						.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				Parser parser = new Parser(request, response, "error");
				parser.simpleParse(false);
			}
		}
	}

	public static void notExist(HttpServletRequest request) throws Exception {
		throw new Exception(Str.format(request, "objectNotExist"));
	}

	public static boolean isFormSubmit(HttpServletRequest request) {
		return request.getParameter("_xwlfm") != null;
	}

	public static boolean checkRight(HttpServletRequest request, String xwlId)
			throws Exception {
		ConcurrentHashMap<String, XwlData> xwlMap = XwlBuffer.getXwlMap();
		String role, userRoles[];
		XwlData d;
		ArrayList<String> setRoles;

		d = xwlMap.get(xwlId);
		if (d == null)
			return false;
		if (!d.isFolder && !d.content.optBoolean("loginRequired", true))
			return true;
		userRoles = (String[]) request.getAttribute("sys.userRoles");
		if (StringUtil.indexOf(userRoles, "admin") != -1)
			return true;
		while (d != null) {
			setRoles = d.roles;
			if (setRoles != null) {
				for (String r : setRoles) {
					role = StringUtil.getNamePart(r);
					if (role.equals("default")
							|| StringUtil.indexOf(userRoles, role) != -1)
						return true;
				}
				return false;
			}
			d = xwlMap.get(d.parentId);
		}
		return true;
	}

	public static boolean checkLogin(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute("sys.logined") == null) {
			if (isFormSubmit(request)) {
				String resp;
				if (Var.getBool("webbuilder.session.loginVerify"))
					resp = "{success:false,value:\"xwlw__login,xwlw__needLV=true\"}";
				else
					resp = "{success:false,value:\"xwlw__login\"}";
				WebUtil.response(response, resp);
			} else {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				Parser parser = new Parser(request, response, "login");
				parser.simpleParse(false);
			}
			return false;
		} else {
			request.setAttribute("sys.user", session.getAttribute("sys.user"));
			request.setAttribute("sys.userName", session
					.getAttribute("sys.userName"));
			request.setAttribute("sys.dispName", session
					.getAttribute("sys.dispName"));
			request.setAttribute("sys.userRoles", session
					.getAttribute("sys.userRoles"));
			String lang = (String) session.getAttribute("sys.lang");
			if (!StringUtil.isEmpty(lang) && !lang.equals("auto"))
				request.setAttribute("sys.lang", lang);
			return true;
		}
	}

	public static String getUrl(String url, boolean onlyId) {
		int idx;

		if (url.startsWith("#")) {
			idx = url.indexOf('(');
			if (idx == -1)
				url = url.substring(1);
			else
				url = url.substring(1, idx);
			if (onlyId)
				return url.trim();
			else
				return "main?xwl=" + url.trim();
		} else
			return url;
	}

	public static String getLanguage(HttpServletRequest request)
			throws Exception {
		String setLan = Var.get("webbuilder.language");
		if (setLan.equalsIgnoreCase("auto")) {
			String acceptLang = request.getHeader("Accept-Language"), language, country;
			if (acceptLang != null) {
				int pos = acceptLang.indexOf(',');
				if (pos != -1)
					acceptLang = acceptLang.substring(0, pos);
				pos = acceptLang.indexOf(';');
				if (pos != -1)
					acceptLang = acceptLang.substring(0, pos);
				pos = acceptLang.indexOf('-');
				if (pos == -1)
					return acceptLang.toLowerCase();
				else {
					language = acceptLang.substring(0, pos).toLowerCase();
					country = acceptLang.substring(pos + 1).toUpperCase();
					return language + "_" + country;
				}
			}
			return Var.get("webbuilder.defaultLanguage");
		} else
			return setLan;
	}

	public static void checkNull(HttpServletRequest request, String list)
			throws Exception {
		if (StringUtil.isEmpty(list))
			return;
		String l[] = list.split(",");
		for (String n : l)
			if (StringUtil.isEmpty(request.getParameter(n)))
				SysUtil.error(Str.format(request, "nullValue", n));
	}

	public static String getIdWithUser(HttpServletRequest request, String id)
			throws Exception {
		String user = (String) request.getAttribute("sys.user");
		return StringUtil.concat(StringUtil.optString(user), "@", id);
	}

	public static void print(HttpServletRequest request, Object s) {
		innerPrint(request, s, false);
	}

	public static void println(HttpServletRequest request, Object s) {
		innerPrint(request, s, true);
	}

	private static void innerPrint(HttpServletRequest request, Object s,
			boolean addLn) {
		HttpSession session = request.getSession(false);

		if (session != null) {
			QueueWriter out = (QueueWriter) session.getAttribute("sys.out");
			if (out != null) {
				if (addLn)
					out.println(s);
				else
					out.print(s);
			}
		}
	}

	public static String fetch(HttpServletRequest request, String name) {
		Object obj = request.getAttribute(name);
		String val;

		if (obj == null) {
			val = request.getParameter(name);
			if (val == null)
				return "";
			else
				return val;
		} else
			return obj.toString();
	}

	public static void response(HttpServletResponse response, Object obj)
			throws Exception {
		if (obj instanceof InputStream) {
			InputStream is = (InputStream) obj;
			try {
				SysUtil.isToOs(is, response.getOutputStream());
			} finally {
				is.close();
			}
		} else {
			byte[] bytes;
			if (obj instanceof byte[])
				bytes = (byte[]) obj;
			else
				bytes = obj.toString().getBytes("utf-8");
			int len = bytes.length;
			if (len >= Var.getInt("server.respGzipMinSize")) {
				response.setHeader("Content-Encoding", "gzip");
				GZIPOutputStream gos = new GZIPOutputStream(response
						.getOutputStream());
				try {
					gos.write(bytes);
				} finally {
					gos.close();
				}
			} else {
				response.setContentLength(len);
				response.getOutputStream().write(bytes);
			}
		}
		response.flushBuffer();
	}

	public static void response(HttpServletResponse response, String obj,
			boolean successful) throws Exception {
		WebUtil.response(response, StringUtil.concat("{success:", Boolean
				.toString(successful), ",value:", StringUtil.quote(StringUtil
				.convertHTML(obj)), "}"));
	}
}