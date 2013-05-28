package com.webbuilder.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.controls.Control;
import com.webbuilder.controls.FrontControl;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.LogUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.SysUtil;
import com.webbuilder.utils.WebUtil;

public class Parser {
	public StringBuilder header = new StringBuilder();
	public Stack<String> footer = new Stack<String>();
	public StringBuilder headerScript = new StringBuilder();
	public Stack<String> footerScript = new Stack<String>();
	public StringBuilder initScript = new StringBuilder();
	public StringBuilder finalScript = new StringBuilder();
	private boolean loadLib;
	private String xwlId;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private JSONObject content;
	private XwlData xwlData;

	public Parser(HttpServletRequest req, HttpServletResponse resp, String xwl)
			throws Exception {
		request = req;
		response = resp;
		xwlId = xwl;
		xwlData = XwlBuffer.getXwl(xwlId);
		if (xwlData == null)
			content = null;
		else
			content = xwlData.content;
	}

	public void parse() throws Exception {
		if (content == null) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			WebUtil.response(response, Str.langFormat(WebUtil
					.getLanguage(request), "moduleNotFound"));
			return;
		}
		if (!checkMethod())
			return;
		boolean ex = false, logAll, loginRequired, created;
		String logType, logMsg, tokens;
		List<?> list = null;

		logType = gs("logType");
		logAll = logType.equals("all");
		loginRequired = content.optBoolean("loginRequired", true);
		tokens = gs("tokens");
		if (!tokens.isEmpty() && checkToken(tokens))
			loginRequired = false;
		try {
			storeVar();
			if (loginRequired) {
				if (WebUtil.checkLogin(request, response)) {
					if (!WebUtil.checkRight(request, xwlId))
						throw new Exception(Str.format(request, "forbidden"));
				} else
					return;
			}
			if (ServletFileUpload.isMultipartContent(request))
				list = WebUtil.setUploadFile(request);
			logMsg = gs("logMessage");
			if (!logMsg.isEmpty())
				LogUtil.message(request, logMsg);
			else if (logAll || logType.equals("access"))
				LogUtil.message(request, StringUtil.concat(xwlData.title, "(",
						xwlId, ")"));
			created = createModule(true);
			createChildren(content);
			doFinalize();
			outputScript(created);
		} catch (Throwable e) {
			ex = true;
			if (logAll || logType.equals("exception"))
				LogUtil.error(request, StringUtil.concat(xwlData.title, "(",
						xwlId, "): ", SysUtil.getShortError(e)));
			WebUtil.showException(e, request, response);
		} finally {
			closeObjects(request, ex);
			if (list != null)
				WebUtil.clearUploadFile(request, list);
		}
	}

	public void simpleParse(boolean asComp) throws Exception {
		boolean created = createModule(!asComp);
		createChildren(content);
		doFinalize();
		if (!asComp)
			outputScript(created);
	}

	private boolean checkToken(String tokens) {
		String token = WebUtil.fetch(request, "_token").trim();
		if (token.isEmpty())
			return false;
		String[] ls = StringUtil.split(tokens, ",");
		for (String s : ls) {
			if (token.equals(s.trim()))
				return true;
		}
		return false;
	}

	private void doFinalize() throws Exception {
		String modules = gs("finalModules"), script = gs("finalScript"), method = gs("finalMethod");
		if (!modules.isEmpty())
			importModules(modules);
		if (!script.isEmpty())
			ScriptBuffer.run(StringUtil.concat(xwlId, ".", gs("id"),
					".finalScript"), script, request, response);
		if (!method.isEmpty())
			SysUtil.executeMethod(method, request, response);
	}

	private void storeVar() throws Exception {
		Date date = new Date();

		request.setAttribute("sys.date", date);
		request.setAttribute("sys.now", DateUtil.toString(date));
		request.setAttribute("sys.id", SysUtil.getId());
		request.setAttribute("sys.lang", WebUtil.getLanguage(request));
	}

	private void createChildren(JSONObject obj) throws Exception {
		String metaName, className, parentMeta = obj.optString("xwlMeta");
		JSONObject jo, meta, xwlMeta;
		JSONArray ja;
		Class<?> cls;
		Control control;
		int i, j;
		boolean isFrontControl;

		ja = obj.optJSONArray("children");
		if (ja == null)
			return;
		j = ja.length();
		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			metaName = jo.optString("xwlMeta");
			meta = XwlBuffer.getMeta(metaName);
			xwlMeta = meta.getJSONObject("xwlMeta");
			className = xwlMeta.optString("xwlClass");
			if (className.indexOf('.') == -1)
				cls = Class.forName("com.webbuilder.controls." + className);
			else
				cls = Class.forName(className);
			control = (Control) cls.newInstance();
			control.setXwlMeta(xwlMeta);
			control.setPropertyList(meta.optJSONObject("properties"));
			control.xwlId = xwlId;
			control.request = request;
			control.response = response;
			control.xwlObject = jo;
			control.hasChild = jo.has("children");
			control.parentControl = obj;
			control.hasParent = !StringUtil.isEqual(parentMeta, "folder")
					&& !StringUtil.isEqual(parentMeta, "module");
			control.isFirstChild = control.hasParent && i == 0;
			control.create();
			isFrontControl = control instanceof FrontControl;
			if (isFrontControl) {
				FrontControl fc = (FrontControl) control;
				appendScript(header, fc.getHeader());
				footer.push(fc.getFooter());
				appendScript(headerScript, fc.getHeaderScript());
				footerScript.push(fc.getFooterScript());
				appendScript(initScript, fc.getInitScript());
				insertScript(finalScript, fc.getFinalScript());
			}
			if (control.hasChild)
				createChildren(jo);
			if (isFrontControl)
				appendScript(headerScript, footerScript.pop());
			if (isFrontControl)
				appendScript(header, footer.pop());
		}
	}

	private boolean checkMethod() throws Exception {
		String method = gs("method");
		return method.isEmpty() || method.equals(request.getMethod());
	}

	private String gs(String name) throws Exception {
		return StringUtil.replaceParameters(request, content.optString(name));
	}

	private boolean gb(String name, boolean defaultVal) throws Exception {
		String v = gs(name);

		if (StringUtil.isEmpty(v))
			return defaultVal;
		else
			return StringUtil.getBool(v);
	}

	private void importModules(String modules) throws Exception {
		String[] list;

		if (modules.substring(0, 1).equals("[")) {
			JSONArray ja = new JSONArray(modules);
			int i, j = ja.length();
			list = new String[j];
			for (i = 0; i < j; i++)
				list[i] = WebUtil.getUrl(ja.getString(i), true);
		} else {
			list = new String[1];
			list[0] = WebUtil.getUrl(modules, true);
		}
		for (String m : list) {
			Parser p = new Parser(request, response, m);
			p.simpleParse(true);
			appendScript(header, p.header.toString());
			appendScript(headerScript, p.headerScript.toString());
			appendScript(initScript, p.initScript.toString());
			appendScript(finalScript, p.finalScript.toString());
		}
	}

	private void createBody() throws Exception {
		ArrayList<String> cssFiles = new ArrayList<String>();
		ArrayList<String> jsFiles = new ArrayList<String>();
		String lang = null;

		loadLib = gb("loadLib", true);
		if (loadLib) {
			lang = (String) request.getAttribute("sys.lang");
			cssFiles.add("webbuilder/controls/ext/resources/css/ext-all.css");
			cssFiles.add("webbuilder/css/style.css");
			jsFiles.add("webbuilder/controls/ext/ext-all.js");
			jsFiles.add(StringUtil.concat(
					"webbuilder/controls/ext/locale/ext-lang-", Str
							.optExtLanguage(lang), ".js"));
			jsFiles.add("webbuilder/script/wb.js");
			Calendar cal = Calendar.getInstance();
			int zoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal
					.get(Calendar.DST_OFFSET)) / 60000;
			String ajaxTimeout = Var.get("webbuilder.session.ajaxTimeout");
			if (StringUtil.isEmpty(ajaxTimeout))
				ajaxTimeout = null;
			appendScript(initScript, StringUtil.concat("Wb.initialize(",
					ajaxTimeout, ",", Integer.toString(zoneOffset), ");"));
		}
		addUserFile(cssFiles, gs("cssFiles"));
		addUserFile(jsFiles, gs("jsFiles"));
		appendScript(
				header,
				"<!DOCTYPE html>\n<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\">\n<title>");
		header.append(gs("title"));
		header.append("</title>");
		if (loadLib) {
			header
					.append("\n<script type=\"text/javascript\" src=\"webbuilder/script/locale/wb-lang-");
			header.append(Str.optLanguage(lang));
			header
					.append(".js\"></script>\n<script type=\"text/javascript\" src=\"webbuilder/script/welcome.js\"></script>");
		}
		for (String s : cssFiles) {
			appendScript(header, "<link rel=\"stylesheet\" href=\"");
			header.append(s);
			header.append("\" type=\"text/css\">");
		}
		for (String s : jsFiles) {
			appendScript(header, "<script type=\"text/javascript\" src=\"");
			header.append(s);
			header.append("\"></script>");
		}
		appendScript(header, gs("head"));
		appendScript(header, "</head>\n<body");
		String cls = gs("class"), style = gs("style"), tp = gs("tagProperties");
		if (!StringUtil.isEmpty(cls)) {
			header.append(" class=\"");
			header.append(cls);
			header.append("\"");
		}
		if (!StringUtil.isEmpty(style)) {
			header.append(" style=\"");
			header.append(style);
			header.append("\"");
		}
		if (!StringUtil.isEmpty(tp)) {
			header.append(' ');
			header.append(tp);
		}
		header.append('>');
		appendScript(initScript, gs("initialize"));
		appendScript(finalScript, gs("finalize"));
		populateEvents();
	}

	private boolean createModule(boolean create) throws Exception {
		String method = gs("initMethod"), script = gs("initScript"), modules = gs("initModules");
		boolean result;

		if (!method.isEmpty())
			SysUtil.executeMethod(method, request, response);
		if (!script.isEmpty())
			ScriptBuffer.run(StringUtil.concat(xwlId, ".", gs("id"),
					".initScript"), script, request, response);
		if (create && gb("createBody", true)) {
			createBody();
			result = true;
		} else
			result = false;
		if (!modules.isEmpty())
			importModules(modules);
		return result;
	}

	private void addUserFile(ArrayList<String> list, String url)
			throws Exception {
		if (!StringUtil.isEmpty(url)) {
			if (url.startsWith("[")) {
				JSONArray ja = new JSONArray(url);
				int i, j = ja.length();
				for (i = 0; i < j; i++)
					list.add(WebUtil.getUrl(ja.getString(i), false));
			} else
				list.add(WebUtil.getUrl(url, false));
		}
	}

	private void populateEvents() throws Exception {
		String resize = gs("resize"), unload = gs("beforeunload");

		if (!resize.isEmpty()) {
			appendScript(headerScript, "window.onresize=function(){\n");
			headerScript.append(resize);
			headerScript.append("\n}");
		}
		if (!unload.isEmpty()) {
			appendScript(headerScript, "Wd.wb_beforeunload=function(){\n");
			headerScript.append(unload);
			headerScript
					.append("\n}\nwindow.onbeforeunload=function(){if(!Wd.wb_forceCls&&!Wb.isLogout())return wb_beforeunload();}");
		}
	}

	private void appendScript(StringBuilder buf, String script) {
		if (!StringUtil.isEmpty(script)) {
			if (buf.length() > 0)
				buf.append("\n");
			buf.append(script);
		}
	}

	private void insertScript(StringBuilder buf, String script) {
		if (!StringUtil.isEmpty(script)) {
			if (buf.length() > 0)
				buf.insert(0, "\n");
			buf.insert(0, script);
		}
	}

	private void outputScript(boolean suffix) throws Exception {
		if (response.isCommitted())
			return;
		int l1, l2, l3;
		l1 = headerScript.length();
		l2 = initScript.length();
		l3 = finalScript.length();
		if (l1 + l2 + l3 > 0) {
			header
					.append("\n<script language=\"javascript\" type=\"text/javascript\">");
			if (loadLib)
				header.append("\nExt.onReady(function(){");
			if (l2 > 0) {
				header.append("\n");
				header.append(initScript);
			}
			if (l1 > 0) {
				header.append("\n");
				header.append(headerScript);
			}
			if (l3 > 0) {
				header.append("\n");
				header.append(finalScript);
			}
			if (loadLib)
				header.append("\n});");
			header.append("\n</script>");
		}
		if (suffix)
			header.append("\n</body>\n</html>");
		if (WebUtil.isFormSubmit(request))
			WebUtil.response(response, "{success:true,value:null}");
		else if (header.length() > 0)
			WebUtil.response(response, header);
	}

	private void closeObjects(HttpServletRequest request, boolean isExcept) {
		Enumeration<?> enums = request.getAttributeNames();
		Object object;
		String attrName;
		ArrayList<Connection> connList = new ArrayList<Connection>();

		while (enums.hasMoreElements()) {
			attrName = enums.nextElement().toString();
			object = request.getAttribute(attrName);
			if (object != null) {
				if (object instanceof ResultSet)
					DbUtil.closeResultSet((ResultSet) object);
				else if (object instanceof Connection)
					connList.add((Connection) object);
			}
		}
		for (Connection conn : connList)
			DbUtil.closeConnection(conn, isExcept);
	}
}