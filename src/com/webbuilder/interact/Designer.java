package com.webbuilder.interact;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Main;
import com.webbuilder.common.ScriptBuffer;
import com.webbuilder.common.Str;
import com.webbuilder.common.Value;
import com.webbuilder.common.Var;
import com.webbuilder.common.XwlBuffer;
import com.webbuilder.common.XwlData;
import com.webbuilder.tool.PageInfo;
import com.webbuilder.tool.QueueWriter;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.SysUtil;
import com.webbuilder.utils.WebUtil;

public class Designer {
	public static void exportPack(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		boolean found, expAll;
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		Set<Entry<String, XwlData>> es = map.entrySet();
		StringBuilder buf = new StringBuilder();
		String id, root = request.getParameter("root"), title = Var
				.get("webbuilder.title");
		XwlData data, current;

		expAll = root.equals("-1");
		buf.append("[");
		if (root.equals("-1"))
			buf.append("{id:\"-1\"}");
		else {
			data = map.get(root);
			title = StringUtil.replaceParameters(request, data.title);
			if (data.parentId.equals("-1"))
				buf.append("{id:\"-1\"}");
			else {
				buf.append("{id:\"");
				buf.append(data.parentId);
				buf.append("\",date:\"");
				data = map.get(data.parentId);
				buf.append(StringUtil.substring(DateUtil
						.toString(data.createDate), 0, 19));
				buf.append("\"}");
			}
		}
		for (Entry<String, XwlData> e : es) {
			id = e.getKey();
			data = e.getValue();
			found = false;
			if (expAll || root.equals(id))
				found = true;
			else {
				current = data;
				do {
					if (root.equals(current.parentId)) {
						found = true;
						break;
					}
				} while ((current = map.get(current.parentId)) != null);
			}
			if (!found)
				continue;
			buf.append(",{MODULE_ID:");
			buf.append(StringUtil.quote(id));
			buf.append(",PARENT_ID:");
			buf.append(StringUtil.quote(data.parentId));
			buf.append(",MODULE_CONTENT:");
			if (data.content == null)
				buf.append("null");
			else
				buf.append(StringUtil.quote(data.content.toString()));
			buf.append(",DISPLAY_NAME:");
			buf.append(StringUtil.quote(data.title));
			buf.append(",DISPLAY_ICON:");
			buf.append(StringUtil.quote(data.icon));
			buf.append(",IS_HIDDEN:");
			buf.append(data.isHidden ? 1 : 0);
			buf.append(",NEW_WIN:");
			buf.append(data.newWin ? 1 : 0);
			buf.append(",CREATE_DATE:\"");
			buf.append(DateUtil.toString(data.createDate));
			buf.append("\",LAST_MODIFY_DATE:\"");
			buf.append(DateUtil.toString(data.lastModifyDate));
			buf.append("\",ORDER_INDEX:");
			buf.append(data.orderIndex);
			buf.append("}");
		}
		buf.append("]");
		response.reset();
		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;"
				+ WebUtil.encodeFilename(request, title + ".gz"));
		OutputStream os = new GZIPOutputStream(response.getOutputStream());
		try {
			os.write(buf.toString().getBytes("utf-8"));
		} finally {
			os.close();
		}
		response.flushBuffer();
	}

	public static void initPack(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		StringBuilder buf = new StringBuilder();
		JSONArray ja = preparePack(request, buf), updateJa = new JSONArray(), insertJa = new JSONArray();
		JSONObject jo;
		int i, j = ja.length();

		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			if (jo.optInt("isCancel") != 1) {
				if (jo.optInt("isUpdate") == 1)
					updateJa.put(jo);
				else
					insertJa.put(jo);
			}
		}
		request.setAttribute("updateArray", updateJa);
		request.setAttribute("insertArray", insertJa);
		request.setAttribute("replaceIds", buf.toString());
	}

	private static JSONArray preparePack(HttpServletRequest request,
			StringBuilder replacedIds) throws Exception {
		String content;
		InputStream is = new GZIPInputStream((InputStream) request
				.getAttribute("uploadFile"));
		try {
			content = StringUtil.getUtfString(is);
		} finally {
			is.close();
		}
		JSONArray ja = new JSONArray(content);
		JSONObject jo;
		String id, moduleId, orgDate, oldParentId = null, newOrgDate, modifyDate, newModifyDate, src, dst;
		int i, j = ja.length();
		boolean isFirst = true, renewParentId = false;
		ArrayList<String> list = new ArrayList<String>();
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		XwlData data;

		jo = ja.getJSONObject(0);
		moduleId = jo.optString("id");
		data = map.get(moduleId);
		if (!moduleId.equals("-1")
				&& (data == null || !StringUtil.substring(
						DateUtil.toString(data.createDate), 0, 19).equals(
						jo.opt("date")))) {
			oldParentId = moduleId;
			renewParentId = true;
		}
		for (i = 1; i < j; i++) {
			jo = ja.getJSONObject(i);
			moduleId = jo.getString("MODULE_ID");
			if (renewParentId
					&& StringUtil.isEqual(oldParentId, jo
							.getString("PARENT_ID")))
				jo.put("PARENT_ID", "-1");
			data = map.get(moduleId);
			if (data != null) {
				orgDate = StringUtil.substring(DateUtil
						.toString(data.createDate), 0, 19);
				modifyDate = StringUtil.substring(DateUtil
						.toString(data.lastModifyDate), 0, 19);
				newOrgDate = StringUtil.substring(JsonUtil.optString(jo,
						"CREATE_DATE"), 0, 19);
				newModifyDate = StringUtil.substring(JsonUtil.optString(jo,
						"LAST_MODIFY_DATE"), 0, 19);
				if (orgDate.equals(newOrgDate) || moduleId.length() < 12) {
					if (modifyDate.equals(newModifyDate))
						jo.put("isCancel", 1);
					else
						jo.put("isUpdate", 1);
				} else {
					id = SysUtil.getId();
					list.add(id + "=" + moduleId);
					jo.put("MODULE_ID", id);
				}
			}
		}
		ja.remove(0);
		content = ja.toString();
		for (String s : list) {
			src = StringUtil.getValuePart(s);
			dst = StringUtil.getNamePart(s);
			content = StringUtil.replace(content, src, dst);
			if (isFirst)
				isFirst = false;
			else
				replacedIds.append(", ");
			replacedIds.append(src);
			replacedIds.append("-&gt;");
			replacedIds.append(dst);
		}
		return new JSONArray(content);
	}

	public static void impModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String content = StringUtil.getUtfString((InputStream) request
				.getAttribute("uploadFile"));
		JSONObject jo = new JSONObject(content);
		request.setAttribute("content", content);
		DbUtil
				.update(
						request,
						"update WB_MODULE set MODULE_CONTENT={?text.content?},LAST_MODIFY_USER={?sys.user?},LAST_MODIFY_DATE={?sys.date?} where MODULE_ID={?moduleId?}");
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		String id = (String) request.getAttribute("moduleId");
		ScriptBuffer.remove(id);
		XwlData data = map.get(id);
		data.content = jo;
		data.lastModifyUser = (String) request.getAttribute("sys.user");
		data.lastModifyDate = (Date) request.getAttribute("sys.date");
	}

	public static void expModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.reset();
		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;"
				+ WebUtil.encodeFilename(request, request.getParameter("title")
						+ ".xwl"));
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		String id = request.getParameter("moduleId");
		XwlData data = map.get(id);
		WebUtil.response(response, data.content.toString());
	}

	public static void search(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		boolean useRegExp = StringUtil
				.getBool(request.getParameter("regCheck"));
		boolean caseSens = StringUtil
				.getBool(request.getParameter("caseCheck"));
		boolean searchAll = StringUtil
				.getBool(request.getParameter("allCheck"));
		boolean onlyKey = StringUtil.getBool(request.getParameter("keyCheck"));
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		Set<Entry<String, XwlData>> es = map.entrySet();
		StringBuilder buf = new StringBuilder();
		XwlData data;
		String title, searchText = request.getParameter("searchText");
		Pattern pattern;
		PageInfo pageInfo = WebUtil.getPage(request);
		String key, moduleId = request.getParameter("module"), iconCls;

		if (StringUtil.isEmpty(moduleId))
			searchAll = true;
		if (useRegExp)
			pattern = Pattern.compile(searchText);
		else {
			pattern = null;
			if (!caseSens)
				searchText = searchText.toLowerCase();
		}
		buf.append(",rows:[");
		for (Entry<String, XwlData> e : es) {
			key = e.getKey();
			if (!searchAll && !key.equals(moduleId))
				continue;
			data = e.getValue();
			if (!data.isFolder) {
				title = StringUtil.replaceParameters(request, data.title);
				iconCls = data.icon;
				if (!searchJson(data.content, buf, searchText, pattern,
						onlyKey, caseSens, title, key, pageInfo, iconCls))
					break;
			}
		}
		buf.append("]}");
		WebUtil.setTotal(buf, pageInfo);
		WebUtil.response(response, buf);
	}

	private static boolean searchJson(JSONObject jo, StringBuilder buf,
			String searchText, Pattern pattern, boolean onlyKey,
			boolean caseSens, String module, String moduleId,
			PageInfo pageInfo, String iconCls) throws Exception {
		Iterator<?> it;
		String name, value, text, objId;
		JSONArray ja;
		int i, j, k = 0, l, cp, ct;
		Matcher matcher = null;
		it = jo.keys();
		while (it.hasNext()) {
			name = (String) it.next();
			if (name.equals("xwlMeta"))
				continue;
			if (name.equals("children")) {
				ja = jo.getJSONArray("children");
				j = ja.length();
				for (i = 0; i < j; i++)
					searchJson(ja.getJSONObject(i), buf, searchText, pattern,
							onlyKey, caseSens, module, moduleId, pageInfo,
							iconCls);
			} else {
				value = jo.optString(name);
				if (onlyKey)
					text = name;
				else
					text = value;
				if (pattern == null && !caseSens)
					text = text.toLowerCase();
				if (pattern == null && (k = text.indexOf(searchText)) != -1
						|| pattern != null
						&& (matcher = pattern.matcher(text)).find()) {
					cp = WebUtil.checkPage(pageInfo);
					if (cp == 1)
						return false;
					else if (cp == 2)
						continue;
					if (pageInfo.count > pageInfo.start + 1)
						buf.append(",");
					objId = jo.optString("id");
					buf.append("{value:");
					if (!onlyKey) {
						if (pattern == null)
							l = k + searchText.length();
						else {
							k = matcher.start();
							l = matcher.end();
						}
						value = StringUtil.concat(StringUtil.toHTML(value
								.substring(Math.max(0, k - 30), k), false,
								false), "<b>", StringUtil.toHTML(value
								.substring(k, l), false, false), "</b>",
								StringUtil.toHTML(value.substring(l), false,
										false));
					}
					if (pattern == null)
						ct = StringUtil.stringOccur(text, searchText);
					else {
						ct = 1;
						while (matcher.find())
							ct++;
					}
					buf.append(StringUtil.quote(StringUtil.concat("(", Integer
							.toString(ct), ") ", value)));
					buf.append(",name:");
					buf.append(StringUtil.quote(StringUtil.concat(objId, ".",
							name)));
					buf.append(",module:");
					buf.append(StringUtil.quote(module));
					buf.append(",moduleId:");
					buf.append(StringUtil.quote(moduleId));
					buf.append(",iconCls:");
					buf.append(StringUtil.quote(iconCls));
					buf.append(",objId:");
					buf.append(StringUtil.quote(objId));
					buf.append("}");
				}
			}
		}
		return true;
	}

	public static void getOutputs(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String s = "";
		HttpSession session = request.getSession(true);
		QueueWriter out = (QueueWriter) session.getAttribute("sys.out");

		if (out == null) {
			out = new QueueWriter(Var.getInt("webbuilder.app.ide.consoleSize"));
			session.setAttribute("sys.out", out);
		} else {
			s = out.toString();
			out.clear();
		}
		WebUtil.response(response, s);
	}

	public static void saveModuleTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		Set<Entry<String, XwlData>> es = map.entrySet();
		String parentId = request.getParameter("parentId"), srcId = request
				.getParameter("srcId");
		int orderIndex = Integer.parseInt(request.getParameter("orderIndex"));
		XwlData data;

		for (Entry<String, XwlData> e : es) {
			data = e.getValue();
			if (StringUtil.isEqual(data.parentId, parentId)
					&& data.orderIndex >= orderIndex)
				data.orderIndex = data.orderIndex + 1;
		}
		data = map.get(srcId);
		if (data == null)
			WebUtil.notExist(request);
		else {
			data.parentId = parentId;
			data.orderIndex = orderIndex;
		}
	}

	public static void pasteModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		boolean isCut = StringUtil.getBool(request.getParameter("isCut"));
		String id = request.getParameter("id"), parentId = request
				.getParameter("parentId"), newId;
		int orderIndex = Integer.parseInt(request.getParameter("orderIndex"));
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		Set<Entry<String, XwlData>> es = map.entrySet();
		Connection conn = null;
		PreparedStatement stm = null, stmDel = null;
		ArrayList<String> copiedList = new ArrayList<String>();

		try {
			String user = (String) request.getAttribute("sys.user");
			Date date = new Date();
			conn = DbUtil.getConnection();
			conn.setAutoCommit(false);
			stm = conn
					.prepareStatement("insert into WB_MODULE (MODULE_ID,PARENT_ID,MODULE_CONTENT,DISPLAY_NAME,DISPLAY_ICON,IS_HIDDEN,NEW_WIN,CREATE_USER,CREATE_DATE,LAST_MODIFY_USER,LAST_MODIFY_DATE,ORDER_INDEX) values(?,?,?,?,?,?,?,?,?,?,?,?)");
			stmDel = conn
					.prepareStatement("delete from WB_MODULE where MODULE_ID=?");
			newId = copyModule(stm, stmDel, map, es, id, copiedList, isCut,
					parentId, orderIndex, user, date);
			stmDel.executeBatch();
			stm.executeBatch();
			WebUtil.response(response, StringUtil.concat("{id:\"", newId,
					"\",user:\"", user, "\",date:\"", DateUtil.toString(date)
							+ "\"}"));
			conn.commit();
		} catch (Throwable e) {
			if (!isCut)
				for (String s : copiedList)
					map.remove(s);
			throw new Exception(e);
		} finally {
			DbUtil.closeStatement(stm);
			DbUtil.closeStatement(stmDel);
			DbUtil.closeConnection(conn);
		}
	}

	private static String copyModule(PreparedStatement stm,
			PreparedStatement stmDel, ConcurrentHashMap<String, XwlData> map,
			Set<Entry<String, XwlData>> es, String id,
			ArrayList<String> newList, boolean isCut, String parentId,
			int orderIndex, String user, Date date) throws Exception {
		XwlData data = map.get(id), newData = new XwlData();
		String newId;
		Timestamp tsCreate, tsModified;

		if (isCut)
			newId = id;
		else
			newId = SysUtil.getId();
		if (data == null)
			throw new Exception(Str.format("objectNotExist"));
		if (!data.isFolder)
			newData.content = new JSONObject(data.content.toString());
		if (isCut) {
			newData.createDate = data.createDate;
			newData.lastModifyDate = data.lastModifyDate;
			newData.createUser = data.createUser;
			newData.lastModifyUser = data.lastModifyUser;
		} else {
			newData.createDate = date;
			newData.lastModifyDate = date;
			newData.createUser = user;
			newData.lastModifyUser = user;
		}
		tsCreate = new Timestamp(newData.createDate.getTime());
		newData.icon = data.icon;
		newData.isFolder = data.isFolder;
		newData.isHidden = data.isHidden;
		newData.newWin = data.newWin;
		tsModified = new Timestamp(newData.lastModifyDate.getTime());
		if (orderIndex == -1)
			newData.orderIndex = data.orderIndex;
		else
			newData.orderIndex = orderIndex;
		newData.parentId = parentId;
		newData.title = data.title;
		map.put(newId, newData);
		if (!isCut)
			newList.add(newId);
		stm.setString(1, newId);
		stm.setString(2, parentId);
		if (data.isFolder)
			stm.setNull(3, -1);
		else
			DbUtil.setCharStream(stm, 3, newData.content.toString());
		stm.setString(4, newData.title);
		stm.setString(5, newData.icon);
		stm.setInt(6, newData.isHidden ? 1 : 0);
		stm.setInt(7, newData.newWin ? 1 : 0);
		stm.setString(8, user);
		stm.setTimestamp(9, tsCreate);
		stm.setString(10, user);
		stm.setTimestamp(11, tsModified);
		stm.setInt(12, newData.orderIndex);
		stm.addBatch();
		for (Entry<String, XwlData> e : es) {
			if (StringUtil.isEqual(e.getValue().parentId, id)) {
				copyModule(stm, stmDel, map, es, e.getKey(), newList, isCut,
						newId, -1, user, date);
			}
		}
		if (isCut) {
			stmDel.setString(1, id);
			stmDel.addBatch();
		}
		return newId;
	}

	public static void updateModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		String user = (String) request.getAttribute("sys.user");
		Date date = (Date) request.getAttribute("sys.date");
		XwlData data = map.get(request.getParameter("id").toString());

		if (data == null)
			WebUtil.notExist(request);
		data.title = request.getParameter("title");
		data.icon = request.getParameter("icon");
		data.isHidden = StringUtil.getBool(request.getParameter("hidden"));
		data.newWin = StringUtil.getBool(request.getParameter("newWin"));
		data.lastModifyUser = user;
		data.lastModifyDate = date;
		JSONObject jo = new JSONObject();
		jo.put("user", (String) request.getAttribute("sys.user"));
		jo.put("date", (String) request.getAttribute("sys.now"));
		jo.put("title", StringUtil.replaceParameters(request, data.title));
		WebUtil.response(response, jo);
	}

	public static void newModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		Set<Entry<String, XwlData>> es = map.entrySet();
		String user = (String) request.getAttribute("sys.user"), content = request
				.getParameter("content");
		Date date = (Date) request.getAttribute("sys.date");
		XwlData data;
		String id = request.getAttribute("sys.id").toString(), parentId = request
				.getParameter("parentId");
		int orderIndex = Integer.parseInt(request.getParameter("orderIndex"));

		for (Entry<String, XwlData> e : es) {
			data = e.getValue();
			if (StringUtil.isEqual(data.parentId, parentId)
					&& data.orderIndex >= orderIndex)
				data.orderIndex = data.orderIndex + 1;
		}
		data = new XwlData();
		data.parentId = parentId;
		data.isFolder = StringUtil.isEmpty(content);
		if (!data.isFolder)
			data.content = new JSONObject(content);
		data.title = request.getParameter("title");
		data.icon = request.getParameter("icon");
		data.isHidden = StringUtil.getBool(request.getParameter("hidden"));
		data.newWin = StringUtil.getBool(request.getParameter("newWin"));
		data.createUser = user;
		data.lastModifyUser = user;
		data.createDate = date;
		data.lastModifyDate = date;
		data.orderIndex = orderIndex;
		map.put(id, data);
		String info = StringUtil
				.replaceParameters(request,
						"{id:\"{#sys.id#}\",user:\"{#sys.user#}\",date:\"{#sys.now#}\",title:");
		info += StringUtil.quote(StringUtil.replaceParameters(request,
				data.title))
				+ "}";
		WebUtil.response(response, StringUtil.concat("{info:", info, "}"));
	}

	public static void checkParentId(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id = request.getParameter("parentId");

		if (!XwlBuffer.exists(id))
			WebUtil.notExist(request);
	}

	public static void deleteModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id = request.getParameter("id");
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		Set<Entry<String, XwlData>> es = map.entrySet();
		ArrayList<String> delList = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement st1 = null, st2 = null;
		boolean isAll = StringUtil.getBool(request.getParameter("isAll"));

		try {
			conn = DbUtil.getConnection();
			conn.setAutoCommit(false);
			st1 = conn
					.prepareStatement("delete from WB_MODULE where MODULE_ID=?");
			st2 = conn
					.prepareStatement("delete from WB_MODULE_ROLE where MODULE_ID=?");
			addDelModule(st1, st2, map, es, id, delList, isAll);
			st1.executeBatch();
			st2.executeBatch();
			for (String s : delList) {
				ScriptBuffer.remove(s);
				map.remove(s);
			}
			WebUtil.response(response, StringUtil.joinList(delList, ","));
			conn.commit();
		} finally {
			DbUtil.closeStatement(st1);
			DbUtil.closeStatement(st2);
			DbUtil.closeConnection(conn);
		}
	}

	public static void deleteControl(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String name = request.getParameter("META_NAME");
		DbUtil.update(request,
				"delete from WB_META where META_NAME={?META_NAME?}");
		XwlBuffer.getMetaMap().remove(name);
	}

	public static void initialize(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		getIconList(request);
		getJndiList(request);
		setVars(request);
		HttpSession session = request.getSession(true);

		QueueWriter out = (QueueWriter) session.getAttribute("sys.out");
		if (out == null) {
			out = new QueueWriter(Var.getInt("webbuilder.app.ide.consoleSize"));
			session.setAttribute("sys.out", out);
		} else
			out.clear();
	}

	private static void setVars(HttpServletRequest request) throws Exception {
		if (Var.getBool("webbuilder.app.ide.saveLastPath"))
			request.setAttribute("idepath", StringUtil.quote(Value.get(request,
					"wb.ide.path")));
		else
			request.setAttribute("idepath", "\"-\"");
	}

	private static void getIconList(HttpServletRequest request)
			throws Exception {
		File iconPath = new File(Main.path, "webbuilder/images");
		File[] list = iconPath.listFiles();
		FileUtil.sortFiles(list);
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;
		String name;

		buf.append("Ds.iconList=[");
		for (File f : list) {
			if (f.isDirectory())
				continue;
			if (isFirst)
				isFirst = false;
			else
				buf.append(',');
			name = FileUtil.extractFilenameNoExt(f.getName());
			buf.append('"');
			buf.append(name);
			buf.append('"');
		}
		buf.append("];");
		request.setAttribute("dsIconScript", buf);
	}

	private static void getJndiList(HttpServletRequest request)
			throws Exception {
		ArrayList<String> list = DbUtil.listJndi();
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;

		buf.append("Ds.jndiList=[");
		for (String s : list) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(',');
			buf.append(StringUtil.quote(s));
		}
		buf.append("];");
		request.setAttribute("dsJndiScript", buf);
	}

	public static void getControlTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String sql;
		boolean runMode = request.getParameter("r") != null;
		if (runMode)
			sql = " where META_NAME<>'module'";
		else
			sql = "";
		ResultSet rs = DbUtil.query(request,
				"select META_NAME,META_TYPE from WB_META" + sql
						+ " order by ORDER_INDEX");
		StringBuilder buf = new StringBuilder();
		JSONObject jo;
		String oldType = null, name, type;
		boolean isFirst = true, firstSec = true;

		buf.append("{children:[");
		while (rs.next()) {
			name = rs.getString(1);
			type = rs.getString(2);
			if (!StringUtil.isEqual(type, oldType)) {
				oldType = type;
				if (isFirst)
					isFirst = false;
				else {
					buf.append("]},");
				}
				buf.append("{text:");
				buf.append(StringUtil.quote(type));
				buf.append(",children:[");
				firstSec = true;
			}
			if (firstSec)
				firstSec = false;
			else
				buf.append(',');
			jo = new JSONObject("{xwlMeta:\"" + name + "\",id:\"" + name
					+ "\"}");
			setControl(jo, runMode, runMode, false);
			buf.append(jo.toString());
		}
		buf.append("]}]}");
		WebUtil.response(response, buf);
	}

	private static void saveTree(HttpServletRequest request, Connection conn)
			throws Exception {
		JSONArray array = new JSONArray(request.getParameter("metaTree"));
		PreparedStatement stm = conn
				.prepareStatement("update WB_META set META_TYPE=?,ORDER_INDEX=? where META_NAME=?");
		int i, j = array.length();
		JSONObject jo;

		for (i = 0; i < j; i++) {
			jo = array.getJSONObject(i);
			stm.setString(1, jo.getString("META_TYPE"));
			stm.setInt(2, jo.getInt("ORDER_INDEX"));
			stm.setString(3, jo.getString("META_NAME"));
			stm.addBatch();
		}
		stm.executeBatch();
	}

	public static void saveMetaTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Connection conn = DbUtil.getConnection(request);
		conn.setAutoCommit(false);
		saveTree(request, conn);
	}

	public static void getModuleTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String parentId = request.getParameter("parentId");
		boolean check = StringUtil.getBool(WebUtil.fetch(request, "check"));
		boolean role = StringUtil.getBool(WebUtil.fetch(request, "role"));
		int type;

		if (check)
			type = 1;
		else if (role)
			type = 2;
		else
			type = 0;
		WebUtil.response(response,
				getModuleTree(request, parentId, false, type));
	}

	public static void listModules(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String parentId = request.getParameter("parentId");
		WebUtil.response(response, getModuleTree(request, parentId, true, 0));
	}

	public static void getModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id = WebUtil.fetch(request, "id");
		XwlData data = XwlBuffer.getXwl(id);
		if (data == null)
			throw new Exception(Str.format(request, "notFound", id));
		JSONObject objCopy, obj = data.content;

		if (Var.getBool("webbuilder.app.ide.saveLastPath")) {
			String path = request.getParameter("path");
			if (!StringUtil.isEmpty(path))
				Value.set(request, "wb.ide.path", path);
		}
		objCopy = new JSONObject(obj.toString());
		setControl(objCopy, false, true, true);
		WebUtil.response(response, StringUtil.concat("{children:[", objCopy
				.toString(), "]}"));
	}

	public static void getControl(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id = WebUtil.fetch(request, "id");
		JSONObject objCopy, obj = XwlBuffer.getMeta(id);

		if (obj == null)
			throw new Exception(Str.format(request, "notFound", id));
		objCopy = new JSONObject(obj.toString());
		setMeta(objCopy);
		WebUtil.response(response, objCopy);
	}

	public static void saveModule(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Connection conn = DbUtil.getConnection(request);
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		PreparedStatement stm = conn
				.prepareStatement("update WB_MODULE set MODULE_CONTENT=?,LAST_MODIFY_DATE=?,LAST_MODIFY_USER=? where MODULE_ID=?");
		Enumeration<?> enums = request.getParameterNames();
		String p, id, data, xwl, user = (String) request
				.getAttribute("sys.user");
		HashMap<String, String> buffer = new HashMap<String, String>();
		XwlData xd;
		Date date = new Date();
		Timestamp time = new Timestamp(date.getTime());

		conn.setAutoCommit(false);
		while (enums.hasMoreElements()) {
			p = enums.nextElement().toString();
			if (StringUtil.isEqual(StringUtil.substring(p, 0, 4), "xwl_")) {
				id = p.substring(4);
				data = request.getParameter(p);
				DbUtil.setCharStream(stm, 1, data);
				stm.setTimestamp(2, time);
				stm.setString(3, user);
				stm.setString(4, id);
				stm.addBatch();
				buffer.put(id, data);
			}
		}
		stm.executeBatch();
		Set<Entry<String, String>> es = buffer.entrySet();
		for (Entry<String, String> e : es) {
			id = e.getKey();
			xd = map.get(id);
			if (xd == null)
				WebUtil.notExist(request);
			else {
				xwl = e.getValue();
				ScriptBuffer.remove(id);
				xd.content = new JSONObject(xwl);
				xd.lastModifyDate = date;
				xd.lastModifyUser = user;
			}
		}
	}

	public static void saveControl(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Connection conn = DbUtil.getConnection(request);
		PreparedStatement stm = conn
				.prepareStatement("update WB_META set META_CONTENT=? where META_NAME=?");
		Enumeration<?> enums = request.getParameterNames();
		String p, v, id, content;
		HashMap<String, String> buffer = new HashMap<String, String>();

		conn.setAutoCommit(false);
		while (enums.hasMoreElements()) {
			p = enums.nextElement().toString();
			if (StringUtil.isEqual(StringUtil.substring(p, 0, 4), "xwl_")) {
				v = request.getParameter(p);
				id = p.substring(4);
				content = optimizeControl(v);
				DbUtil.setCharStream(stm, 1, content);
				stm.setString(2, id);
				stm.addBatch();
				buffer.put(id, content);
			}
		}
		stm.executeBatch();
		stm.close();
		ConcurrentHashMap<String, JSONObject> map = XwlBuffer.getMetaMap();
		Set<Entry<String, String>> es = buffer.entrySet();
		for (Entry<String, String> e : es)
			map.put(e.getKey(), new JSONObject(e.getValue()));
	}

	public static void newControl(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Connection conn = DbUtil.getConnection(request);
		String name = request.getParameter("META_NAME"), type = request
				.getParameter("META_TYPE");
		String meta = "{xwlMeta:{xwlText:"
				+ StringUtil.quote(name)
				+ ",id:\"string\",xwlIconCls:\"item_icon\"},properties:{id:\"\"}}";
		conn.setAutoCommit(false);
		PreparedStatement stm = conn
				.prepareStatement("insert into WB_META values(?,?,?,?)");
		stm.setString(1, name);
		stm.setString(2, type);
		DbUtil.setCharStream(stm, 3, meta);
		stm.setInt(4, 1);
		stm.executeUpdate();
		saveTree(request, conn);
		XwlBuffer.getMetaMap().put(name, new JSONObject(meta));
		request.setAttribute("id", name);
		getControl(request, response);
	}

	private static String optimizeControl(String v) throws Exception {
		JSONObject jo = new JSONObject(v), json;
		JSONObject m = jo.getJSONObject("xwlMeta");
		JSONObject p = jo.optJSONObject("properties");
		JSONObject e = jo.optJSONObject("events");
		String names[], s, para;

		if (p == null) {
			if (jo.has("properties"))
				jo.remove("properties");
		} else {
			names = JSONObject.getNames(p);
			if (names == null)
				jo.remove("properties");
			else {
				for (String n : names) {
					s = p.getString(n);
					if (!StringUtil.isEmpty(s)) {
						json = new JSONObject(s);
						para = json.optString("parameters");
						if (!StringUtil.isEmpty(para))
							para = "=" + para;
						m.put(n, json.getString("type") + para);
						p.put(n, json.optString("defaultValue"));
					}
				}
			}
		}
		if (e == null) {
			if (jo.has("events"))
				jo.remove("events");
		} else {
			names = JSONObject.getNames(e);
			if (names == null)
				jo.remove("events");
			else {
				for (String n : names) {
					s = e.getString(n);
					if (!StringUtil.isEmpty(s)) {
						json = new JSONObject(s);
						para = json.optString("parameters");
						if (!StringUtil.isEmpty(para))
							para = "=" + para;
						m.put(n, json.getString("type") + para);
						e.put(n, json.optString("defaultValue"));
					}
				}
			}
		}
		return jo.toString();
	}

	private static void setMeta(JSONObject obj) throws Exception {
		JSONObject meta = obj.getJSONObject("xwlMeta");
		JSONObject properties = obj.optJSONObject("properties");
		if (properties == null) {
			properties = new JSONObject();
			obj.put("properties", properties);
		}
		JSONObject events = obj.optJSONObject("events");
		if (events == null) {
			events = new JSONObject();
			obj.put("events", events);
		}
		JSONObject e1 = new JSONObject(), e2 = new JSONObject(), e3 = new JSONObject();
		String name, value, dftValue, content, names[] = JSONObject
				.getNames(meta);
		String edit = "new Ext.form.field.Trigger({enableKeyEvents:true,hideTrigger:true,listeners:{keydown:Cm.monitorKey,change:Cm.monitorChange,focus:Cm.monitorFocus}})";
		String trigger = getTrigger("Cm.editProperty");

		for (String n : names) {
			content = meta.getString(n);
			if (StringUtil.substring(n, 0, 3).equals("xwl"))
				e1.put(n, edit);
			else {
				if (content.indexOf('=') == -1) {
					name = content;
					value = "";
				} else {
					name = StringUtil.getNamePart(content);
					value = StringUtil.getValuePart(content);
				}
				content = "{type:\"" + name + "\"";
				if (!StringUtil.isEmpty(value))
					content += ",parameters:" + StringUtil.quote(value);
				if (properties.has(n)) {
					dftValue = properties.getString(n);
					if (!StringUtil.isEmpty(dftValue))
						content += ",defaultValue:"
								+ StringUtil.quote(dftValue);
					properties.put(n, content + "}");
				} else if (events.has(n)) {
					dftValue = events.getString(n);
					if (!StringUtil.isEmpty(dftValue))
						content += ",defaultValue:"
								+ StringUtil.quote(dftValue);
					events.put(n, content + "}");
				}
				meta.remove(n);
			}
		}
		names = JSONObject.getNames(properties);
		if (names != null) {
			for (String n : names)
				e2.put(n, trigger);
		}
		names = JSONObject.getNames(events);
		if (names != null) {
			for (String n : names)
				e3.put(n, trigger);
		}
		obj.put("custGEditors", e1);
		obj.put("custPEditors", e2);
		obj.put("custEEditors", e3);
	}

	private static void setControl(JSONObject obj, boolean textAsId,
			boolean childAppend, boolean clearDefault) throws Exception {
		String meta = obj.getString("xwlMeta");
		JSONObject metaObj = XwlBuffer.getMeta(meta);
		JSONObject properties, propertyMeta = metaObj
				.optJSONObject("properties");
		if (propertyMeta == null)
			properties = new JSONObject();
		else {
			properties = new JSONObject(propertyMeta.toString());
			if (clearDefault)
				JsonUtil.clear(properties);
		}
		JSONObject events, eventMeta = metaObj.optJSONObject("events");
		if (eventMeta == null)
			events = new JSONObject();
		else {
			events = new JSONObject(eventMeta.toString());
			if (clearDefault)
				JsonUtil.clear(events);
		}
		JSONObject metaConf = metaObj.getJSONObject("xwlMeta");
		JSONObject pEditors = new JSONObject(), empObj = new JSONObject();
		JSONObject eEditors = new JSONObject(), eParaMeta = new JSONObject();
		JSONArray empArray = new JSONArray();
		String names[] = JSONObject.getNames(obj);
		String id = obj.getString("id"), text = metaConf.getString("xwlText");
		String iconCls = metaConf.getString("xwlIconCls");
		boolean isLeaf = true;

		for (String n : names) {
			if (n.equals("children")) {
				JSONArray children = obj.getJSONArray(n);
				int i, j = children.length();
				for (i = 0; i < j; i++)
					setControl(children.getJSONObject(i), textAsId,
							childAppend, clearDefault);
				isLeaf = false;
			} else {
				if (properties != null && properties.has(n)) {
					properties.put(n, obj.get(n));
					obj.remove(n);
				} else if (events != null && events.has(n)) {
					events.put(n, obj.get(n));
					obj.remove(n);
				}
			}
		}
		if (textAsId)
			obj.put("text", text);
		else
			obj.put("text", id);
		obj.put("iconCls", iconCls);
		setXwlMeta(metaConf, obj);
		if (properties == null)
			obj.put("properties", empObj);
		else {
			obj.put("properties", properties);
			String[] nl = JSONObject.getNames(properties);
			if (nl != null) {
				for (String n : nl) {
					setEditors(pEditors, metaConf, n);
					setParaMeta(eParaMeta, metaConf, n);
				}
			}
		}
		if (events == null)
			obj.put("events", empObj);
		else {
			obj.put("events", events);
			String[] nl = JSONObject.getNames(events);
			if (nl != null) {
				for (String n : nl) {
					setEditors(eEditors, metaConf, n);
					setParaMeta(eParaMeta, metaConf, n);
				}
			}
		}
		obj.put("custPEditors", pEditors);
		obj.put("custEEditors", eEditors);
		obj.put("custEPara", eParaMeta);
		if (isLeaf)
			if (!childAppend
					|| StringUtil.isEmpty(metaConf.optString("xwlChildren")))
				obj.put("leaf", true);
			else
				obj.put("children", empArray);
	}

	private static void setXwlMeta(JSONObject meta, JSONObject obj)
			throws Exception {
		String val, ns[] = JSONObject.getNames(meta);
		JSONObject jo = new JSONObject();
		for (String n : ns) {
			val = meta.getString(n);
			if (StringUtil.substring(n, 0, 3).equals("xwl"))
				obj.put(n, val);
			else
				jo.put(n, val);
		}
		obj.put("xwlPT", jo);
	}

	private static String getTrigger(String func) {
		return "new Ext.form.field.Trigger({enableKeyEvents:true,onTriggerClick:"
				+ func
				+ ",listeners:{keydown:Cm.monitorKey,render:Cm.populateDblClick},editable:false,blockPost:true,triggerCls:'ellipsis_icon'})";
	}

	private static void setEditors(JSONObject editors, JSONObject meta,
			String name) throws Exception {
		String value = meta.optString(name), type, para;

		type = StringUtil.getNamePart(value);
		para = StringUtil.getValuePart(value);
		if (StringUtil.isEqual(para, value))
			para = "";
		if (type.equals("js") || type.equals("ss") || type.equals("object")
				|| type.equals("text") || type.equals("sql"))
			editors.put(name, "Ds.edtTrigger('" + type + "')");
		else if (type.equals("boolean"))
			editors.put(name, "Ds.edtBool()");
		else if (type.equals("date"))
			editors.put(name, "Ds.edtDate()");
		else if (type.equals("color"))
			editors.put(name, "Ds.edtColor()");
		else if (type.equals("iconClass"))
			editors.put(name, "Ds.edtIcon()");
		else if (type.equals("jndi"))
			editors.put(name, "Ds.edtJndi()");
		else if (type.equals("bind") || type.equals("bindText"))
			editors.put(name, "Ds.edtBind('" + para + "')");
		else if (type.equals("bindMulti"))
			editors.put(name, "Ds.edtBind('" + para + "',true)");
		else if (type.equals("enum"))
			editors.put(name, "Ds.edtEnum('" + para + "')");
		else if (type.equals("enumMulti"))
			editors.put(name, "Ds.edtEnum('" + para + "',true)");
		else if (type.equals("url"))
			editors.put(name, "Ds.edtUrl(false)");
		else if (type.equals("urlList"))
			editors.put(name, "Ds.edtUrl(true)");
		else {
			if (StringUtil.isEqual(name, "id"))
				editors.put(name, "Ds.edtText('id')");
			else
				editors.put(name, "Ds.edtText('" + type + "')");
		}
	}

	private static void setParaMeta(JSONObject paraMeta, JSONObject meta,
			String name) throws Exception {
		String value = meta.optString(name);
		String type = StringUtil.getNamePart(value);
		String para = StringUtil.getValuePart(value);

		if (type.equals("js") || type.equals("ss"))
			paraMeta.put(name, para);
	}

	private static String getModuleTree(HttpServletRequest request,
			String parentId, boolean runMode, int type) throws Exception {
		ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
		Set<Entry<String, XwlData>> es = map.entrySet();
		StringBuilder buf = new StringBuilder();
		XwlData d;
		boolean isFirst = true, hasRole;
		String id, pid, text;

		if (StringUtil.isEmpty(parentId))
			pid = "-1";
		else
			pid = parentId;
		buf.append("{children:[");
		for (Entry<String, XwlData> e : es) {
			id = e.getKey();
			d = e.getValue();
			if (!StringUtil.isEqual(pid, d.parentId)
					|| runMode
					&& (d.isHidden || !d.isFolder
							&& !WebUtil.checkRight(request, id) || d.isFolder
							&& !canDisplay(request, map, es, id)))
				continue;
			if (isFirst)
				isFirst = false;
			else
				buf.append(',');
			buf.append("{text:");
			text = StringUtil.replaceParameters(request, d.title);
			hasRole = type == 2 && d.roles != null;
			if (hasRole)
				buf.append(StringUtil.quote(StringUtil.concat("<b>", text,
						"</b>")));
			else
				buf.append(StringUtil.quote(text));
			buf.append(",orgTitle:");
			buf.append(StringUtil.quote(text));
			buf.append(",iconCls:\"");
			buf.append(d.icon);
			buf.append("\",leaf:");
			buf.append(!d.isFolder);
			buf.append(",MODULE_ID:\"");
			buf.append(id);
			buf.append("\",PARENT_ID:\"");
			buf.append(d.parentId);
			if (d.isHidden)
				buf.append("\",cls:\"wb_blue");
			buf.append("\",ORDER_INDEX:");
			buf.append(d.orderIndex);
			buf.append(",IS_FOLDER:");
			buf.append(d.isFolder);
			if (hasRole) {
				buf.append(",roles:");
				buf.append(JsonUtil.getText(d.roles));
			}
			buf.append(",NEW_WIN:");
			buf.append(d.newWin);
			if (d.isFolder) {
				if (!folderHasChildren(es, id))
					buf.append(",children:[]");
			} else if (type == 1)
				buf.append(",checked:false");
			if (!runMode) {
				buf.append(",orgText:");
				buf.append(StringUtil.quote(d.title));
				buf.append(",CREATE_USER:\"");
				buf.append(d.createUser);
				buf.append("\",CREATE_DATE:\"");
				buf.append(DateUtil.toString(d.createDate));
				buf.append("\",LAST_MODIFY_USER:\"");
				buf.append(d.createUser);
				buf.append("\",LAST_MODIFY_DATE:\"");
				buf.append(DateUtil.toString(d.lastModifyDate));
				buf.append("\",IS_HIDDEN:");
				buf.append(d.isHidden);
			}
			buf.append('}');
		}
		buf.append("]}");
		return buf.toString();
	}

	private static boolean canDisplay(HttpServletRequest request,
			ConcurrentHashMap<String, XwlData> xwlMap,
			Set<Entry<String, XwlData>> es, String xwlId) {
		String id, role, userRoles[];
		ArrayList<String> setRoles;
		XwlData d, curData;
		boolean isDescendant, hasPerm, notHasDescendant = true, isHidden;

		userRoles = (String[]) request.getAttribute("sys.userRoles");
		for (Entry<String, XwlData> e : es) {
			curData = e.getValue();
			d = curData;
			if (d.isFolder)
				continue;
			isDescendant = false;
			isHidden = false;
			while (((d = xwlMap.get((id = d.parentId))) != null)) {
				if (d.isHidden)
					isHidden = true;
				if (id.equals(xwlId)) {
					if (!isHidden)
						isDescendant = true;
					break;
				}
			}
			if (isDescendant && !curData.isHidden) {
				if (!curData.content.optBoolean("loginRequired", true))
					return true;
				if (notHasDescendant)
					notHasDescendant = false;
				hasPerm = true;
				d = curData;
				do {
					setRoles = d.roles;
					if (setRoles != null) {
						for (String r : setRoles) {
							role = StringUtil.getNamePart(r);
							if (role.equals("default")
									|| StringUtil.indexOf(userRoles, role) != -1)
								return true;
						}
						hasPerm = false;
						break;
					}
					id = d.parentId;
				} while ((d = xwlMap.get(id)) != null);
				if (hasPerm)
					return true;
			}
		}
		if (notHasDescendant)
			return false;
		if (StringUtil.indexOf(userRoles, "admin") != -1)
			return true;
		return false;
	}

	private static boolean folderHasChildren(Set<Entry<String, XwlData>> es,
			String id) {
		for (Entry<String, XwlData> e : es) {
			if (StringUtil.isEqual(e.getValue().parentId, id))
				return true;
		}
		return false;
	}

	private static void addDelModule(PreparedStatement st1,
			PreparedStatement st2, ConcurrentHashMap<String, XwlData> map,
			Set<Entry<String, XwlData>> es, String id,
			ArrayList<String> delList, boolean isAll) throws Exception {
		if (isAll) {
			delList.add(id);
			st1.setString(1, id);
			st1.addBatch();
			st2.setString(1, id);
			st2.addBatch();
		}
		for (Entry<String, XwlData> e : es) {
			if (StringUtil.isEqual(e.getValue().parentId, id)) {
				addDelModule(st1, st2, map, es, e.getKey(), delList, true);
			}
		}
	}
}
