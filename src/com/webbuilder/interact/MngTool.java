package com.webbuilder.interact;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.CollationKey;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Main;
import com.webbuilder.common.Role;
import com.webbuilder.common.ScriptBuffer;
import com.webbuilder.common.Session;
import com.webbuilder.common.Str;
import com.webbuilder.common.SysMap;
import com.webbuilder.common.Var;
import com.webbuilder.common.VarData;
import com.webbuilder.common.XwlBuffer;
import com.webbuilder.common.XwlData;
import com.webbuilder.tool.Encrypter;
import com.webbuilder.tool.PageInfo;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.SysUtil;
import com.webbuilder.utils.WebUtil;

public class MngTool {
	public static void setUserInfo(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.setAttribute("sys.dispName", request
					.getParameter("dispName"));
			session.setAttribute("sys.lang", request.getParameter("language"));
		}
	}

	public static void getLangList(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		File[] fs = (new File(Main.path, "webbuilder/script/locale"))
				.listFiles();
		FileUtil.sortFiles(fs);
		StringBuilder buf = new StringBuilder();
		String name;
		int len;

		buf.append("[\"auto\"");
		for (File f : fs) {
			name = f.getName();
			len = name.length();
			if (len > 11) {
				buf.append(",\"");
				buf.append(name.substring(8, len - 3));
				buf.append("\"");
			}
		}
		buf.append("]");
		request.setAttribute("langList", buf.toString());
	}

	public static void putKey(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		SysMap.put(request.getParameter("KEY_TYPE"), request
				.getParameter("KEY_NAME"), request.getParameter("KEY_VALUE"));
	}

	public static void updateKey(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		SysMap.remove(request.getAttribute("oldKey.KEY_TYPE").toString(),
				request.getAttribute("oldKey.KEY_NAME").toString());
		putKey(request, response);
	}

	public static void removeKey(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(request.getParameter("grid1"));
		JSONObject jo;
		int i, j = ja.length();

		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			SysMap.remove(jo.getString("KEY_TYPE"), jo.getString("KEY_NAME"));
		}
	}

	public static void disableUsers(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, HttpSession> sessionList = Session.sessionList;
		HttpSession session;
		JSONArray ja = new JSONArray(request.getParameter("grid1"));
		int i, j = ja.length(), uIdx = -1;
		JSONObject jo;
		String uid, curUser = (String) request.getAttribute("sys.user");

		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			uid = jo.getString("userId");
			if (uid.equals(curUser))
				uIdx = i;
			else {
				session = sessionList.get(uid);
				if (session != null)
					session.invalidate();
				jo.put("userIdPrefix", uid + "@%");
			}
		}
		if (uIdx != -1)
			ja.remove(uIdx);
		request.setAttribute("jsonArray", ja);
	}

	public static void updateUserRole(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(request.getParameter("roleArray"));
		int i, j = ja.length();
		String roles[];

		if (j == 0)
			roles = null;
		else {
			roles = new String[j];
			for (i = 0; i < j; i++)
				roles[i] = JsonUtil.optString(ja.getJSONObject(i), "id");
		}
		HttpSession session = Session.sessionList.get(request
				.getParameter("userId"));
		if (session != null)
			session.setAttribute("sys.userRoles", roles);
	}

	public static void getPwdSql(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String pwd = request.getParameter("pwd"), key = "^%%rd%%^", user = request
				.getParameter("userName");

		if (user.indexOf('"') != -1 || user.indexOf('\'') != -1)
			throw new Exception(Str.format(request, "invalidName", user));
		if (pwd == null || pwd.length() < 6)
			throw new Exception(Str.format(request, "invalidPwdLen"));
		request.setAttribute("pwd", Encrypter.getMD5(pwd));
		if (request.getAttribute("isUpdate") == null) {
			if (pwd.equals(key))
				throw new Exception(Str.format(request, "invalidPwd"));
		} else if (!pwd.equals(key))
			request.setAttribute("pwdSql", ",PASSWORD={?pwd?}");
	}

	public static void getUsers(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Connection conn = DbUtil.getConnection(request);
		StringBuilder buf = new StringBuilder();
		String orgFields[] = { "USER_NAME", "DISPLAY_NAME", "STATUS",
				"CREATE_DATE", "LOGIN_TIMES", "EMAIL", "LAST_LOGIN" };
		String mapFields[] = { "userName", "dispName", "status", "createDate",
				"loginTimes", "email", "lastLogin" };
		HashMap<String, JSONArray> roleMap = new HashMap<String, JSONArray>();
		JSONArray rows = new JSONArray(), ra;
		JSONObject jo;
		String uid, sortInfo[], orderBy, where, findName, findRole;
		PageInfo pageInfo;
		ResultSet userRs = null, roleRs = null;
		PreparedStatement roleSt = null, userSt = null;
		HttpSession session;
		ConcurrentHashMap<String, HttpSession> sessionList = Session.sessionList;
		int i, len, cp;
		boolean allRoles;

		try {
			sortInfo = WebUtil.getSortInfo(request);
			if (sortInfo == null)
				orderBy = "a.STATUS desc,a.USER_NAME";
			else
				orderBy = "a."
						+ orgFields[StringUtil.indexOf(mapFields, sortInfo[0])]
						+ " " + sortInfo[1];

			findRole = request.getParameter("findRole");
			allRoles = StringUtil.isEqual(findRole, "-1");
			if (!allRoles && !StringUtil.isEmpty(findRole)) {
				userSt = conn
						.prepareStatement("select a.USER_ID,a.USER_NAME,a.DISPLAY_NAME,a.STATUS,a.CREATE_DATE,a.LOGIN_TIMES,a.EMAIL,a.LAST_LOGIN from WB_USER a,WB_USER_ROLE b where a.USER_ID=b.USER_ID and b.ROLE_ID=? order by "
								+ orderBy);
				userSt.setString(1, findRole);
			} else {
				if (allRoles)
					findName = null;
				else
					findName = request.getParameter("findName");
				if (StringUtil.isEmpty(findName))
					where = "";
				else
					where = " where a.USER_NAME like ?";
				userSt = conn
						.prepareStatement("select a.USER_ID,a.USER_NAME,a.DISPLAY_NAME,a.STATUS,a.CREATE_DATE,a.LOGIN_TIMES,a.EMAIL,a.LAST_LOGIN from WB_USER a"
								+ where + " order by " + orderBy);
				if (!StringUtil.isEmpty(findName))
					userSt.setString(1, findName + "%");
			}
			userRs = userSt.executeQuery();
			buf.append(",onlines:");
			buf.append(sessionList.size());
			buf.append(",totalUser:");
			buf.append(request.getAttribute("users.CT"));
			buf.append(",rows:");
			pageInfo = WebUtil.getPage(request);
			while (userRs.next()) {
				cp = WebUtil.checkPage(pageInfo);
				if (cp == 1)
					break;
				else if (cp == 2)
					continue;
				uid = userRs.getString(1);
				jo = new JSONObject();
				jo.put("userId", uid);
				jo.put("userName", userRs.getString(2));
				jo.put("dispName", userRs.getString(3));
				jo.put("status", userRs.getInt(4));
				jo.put("createDate", userRs.getTimestamp(5));
				jo.put("loginTimes", userRs.getInt(6));
				jo.put("email", userRs.getString(7));
				jo.put("lastLogin", userRs.getTimestamp(8));
				session = sessionList.get(uid);
				if (session != null) {
					jo.put("ip", session.getAttribute("sys.userIp"));
					jo.put("on", 1);
				}
				ra = new JSONArray();
				jo.put("roles", ra);
				roleMap.put(uid, ra);
				rows.put(jo);
			}
			len = roleMap.size();
			if (len == 0)
				buf.append("[]");
			else {
				roleSt = conn
						.prepareStatement("select a.USER_ID,a.ROLE_ID,b.ROLE_NAME from WB_USER_ROLE a, WB_ROLE b where a.ROLE_ID=b.ROLE_ID and a.USER_ID in("
								+ StringUtil.duplicate("?,", len - 1) + "?)");
				Set<Entry<String, JSONArray>> es = roleMap.entrySet();
				i = 1;
				for (Entry<String, JSONArray> e : es) {
					roleSt.setString(i++, e.getKey());
				}
				roleRs = roleSt.executeQuery();
				while (roleRs.next()) {
					ra = roleMap.get(roleRs.getString(1));
					ra.put(roleRs.getString(2) + "=" + roleRs.getString(3));
				}
				buf.append(rows);
			}
			buf.append('}');
			WebUtil.setTotal(buf, pageInfo);
			WebUtil.response(response, buf);
		} finally {
			DbUtil.closeResultSet(userRs);
			DbUtil.closeResultSet(roleRs);
			DbUtil.closeStatement(userSt);
			DbUtil.closeStatement(roleSt);
		}
	}

	public static void saveRoleTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, String> map = Role.getRoleMap();
		String id = request.getParameter("itemId"), pi = request
				.getParameter("parentId");

		if (!map.containsKey(id) || !StringUtil.isEqual(pi, "-1")
				&& !map.containsKey(pi))
			throw new Exception(Str.format("notExist", pi));
		DbUtil
				.update(
						request,
						"update WB_ROLE set PARENT_ID={?parentId?} where ROLE_ID={?itemId?}",
						null, true);
		map.put(id, pi + "=" + StringUtil.getValuePart(map.get(id)));
	}

	public static void setRoles(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id = request.getParameter("id"), roleId, roleName;
		if (!XwlBuffer.exists(id))
			WebUtil.notExist(request);
		JSONArray roleIds = new JSONArray(request.getParameter("roleIds"));
		JSONArray roleNames = new JSONArray(request.getParameter("roleNames"));
		int i, j;
		PreparedStatement st1 = null, st2 = null;
		Connection conn = DbUtil.getConnection(request);
		ArrayList<String> list;

		try {
			conn.setAutoCommit(false);
			st1 = conn
					.prepareStatement("delete from WB_MODULE_ROLE where MODULE_ID=?");
			st1.setString(1, id);
			st1.executeUpdate();
			j = roleIds.length();
			if (j > 0) {
				list = new ArrayList<String>();
				st2 = conn
						.prepareStatement("insert into WB_MODULE_ROLE values(?,?)");
				st2.setString(1, id);
				for (i = 0; i < j; i++) {
					roleId = roleIds.optString(i);
					roleName = roleNames.optString(i);
					st2.setString(2, roleId);
					list.add(roleId + "=" + roleName);
					st2.addBatch();
				}
				st2.executeBatch();
			} else
				list = null;
			ConcurrentHashMap<String, XwlData> map = XwlBuffer.getXwlMap();
			XwlData data = map.get(id);
			data.roles = list;
		} finally {
			DbUtil.closeStatement(st1);
			DbUtil.closeStatement(st2);
		}
	}

	public static void updateRole(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, String> buffer = Role.getRoleMap();
		String id = request.getParameter("id"), name = request
				.getParameter("name"), val;
		DbUtil.update(request,
				"update WB_ROLE set ROLE_NAME={?name?} where ROLE_ID={?id?}");
		val = buffer.get(id);
		if (val != null)
			buffer.put(id, StringUtil.getNamePart(val) + "=" + name);
		else
			SysUtil.error(Str.format(request, "notExist", name));
	}

	private static List<Entry<String, String>> sortRole(Map<String, String> map) {
		List<Entry<String, String>> list = new ArrayList<Entry<String, String>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Entry<String, String>>() {
			Collator collator = Collator.getInstance();

			public int compare(Entry<String, String> e1,
					Entry<String, String> e2) {
				CollationKey key1 = collator.getCollationKey(StringUtil
						.getValuePart(e1.getValue()).toLowerCase());
				CollationKey key2 = collator.getCollationKey(StringUtil
						.getValuePart(e2.getValue()).toLowerCase());
				return key1.compareTo(key2);
			}
		});
		return list;
	}

	private static void addDelRole(PreparedStatement st1,
			PreparedStatement st2, PreparedStatement st3,
			ConcurrentHashMap<String, String> map,
			Set<Entry<String, String>> es, String id, ArrayList<String> delList)
			throws Exception {
		delList.add(id);
		st1.setString(1, id);
		st1.addBatch();
		st2.setString(1, id);
		st2.addBatch();
		st3.setString(1, id);
		st3.addBatch();
		for (Entry<String, String> e : es) {
			if (StringUtil.getNamePart(e.getValue()).equals(id)) {
				addDelRole(st1, st2, st3, map, es, e.getKey(), delList);
			}
		}
	}

	public static void deleteRole(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, String> buffer = Role.getRoleMap();
		ArrayList<String> delList = new ArrayList<String>();
		String id = request.getParameter("id");
		Connection conn = null;
		PreparedStatement st1 = null, st2 = null, st3 = null;

		try {
			conn = DbUtil.getConnection(request);
			conn.setAutoCommit(false);
			st1 = conn.prepareStatement("delete from WB_ROLE where ROLE_ID=?");
			st2 = conn
					.prepareStatement("delete from WB_MODULE_ROLE where ROLE_ID=?");
			st3 = conn
					.prepareStatement("delete from WB_USER_ROLE where ROLE_ID=?");
			Set<Entry<String, String>> es = buffer.entrySet();
			addDelRole(st1, st2, st3, buffer, es, id, delList);
			st1.executeBatch();
			st2.executeBatch();
			st3.executeBatch();
			for (String s : delList)
				buffer.remove(s);
			if (!delList.isEmpty()) {
				Set<Entry<String, XwlData>> mes = XwlBuffer.getXwlMap()
						.entrySet();
				XwlData data;
				ArrayList<String> roles;
				int i, j;
				for (Entry<String, XwlData> e : mes) {
					data = e.getValue();
					roles = data.roles;
					if (roles != null && !roles.isEmpty()) {
						j = roles.size();
						for (i = j - 1; i >= 0; i--) {
							if (delList.indexOf(StringUtil.getNamePart(roles
									.get(i))) != -1)
								roles.remove(i);
						}
						if (roles.isEmpty())
							data.roles = null;
					}
				}
			}
		} finally {
			DbUtil.closeStatement(st1);
			DbUtil.closeStatement(st2);
			DbUtil.closeStatement(st3);
		}
	}

	public static void appendRole(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, String> buffer = Role.getRoleMap();
		String id = (String) request.getAttribute("sys.id");
		DbUtil.update(request,
				"insert into WB_ROLE values({?sys.id?},{?parentId?},{?name?})");
		buffer.put(id, request.getParameter("parentId") + "="
				+ request.getParameter("name"));
		WebUtil.response(response, StringUtil.concat("{id:'", id, "'}"));
	}

	public static void getRoleTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String v, id, pid = request.getParameter("itemId");
		StringBuilder buf = new StringBuilder();
		ConcurrentHashMap<String, String> roles = Role.getRoleMap();
		List<Entry<String, String>> es = sortRole(roles);
		boolean isFirst = true, check = StringUtil.getBool(WebUtil.fetch(
				request, "check"));

		if (StringUtil.isEmpty(pid))
			pid = "-1";
		buf.append("{children:[");
		for (Entry<String, String> e : es) {
			v = e.getValue();
			if (StringUtil.getNamePart(v).equals(pid)) {
				if (isFirst)
					isFirst = false;
				else
					buf.append(',');
				id = e.getKey();
				buf.append("{text:");
				buf.append(StringUtil.quote(StringUtil.getValuePart(v)));
				buf.append(",itemId:\"");
				buf.append(id);
				buf.append("\",iconCls:\"user_icon\"");
				if (!hasSubRole(es, id))
					buf.append(",children:[]");
				if (check)
					buf.append(",checked:false");
				buf.append('}');
			}
		}
		buf.append("]}");
		WebUtil.response(response, buf);
	}

	private static boolean hasSubRole(List<Entry<String, String>> es, String id) {
		for (Entry<String, String> e : es) {
			if (StringUtil.isEqual(StringUtil.getNamePart(e.getValue()), id))
				return true;
		}
		return false;
	}

	public static void loadSystem(boolean reload) throws Exception {
		Var.initialize(reload);
		Str.initialize(reload);
		ScriptBuffer.initialize(reload);
		SysMap.initialize(reload);
		Role.initialize(reload);
		XwlBuffer.initialize(reload);
	}

	public static void reloadSystem(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		loadSystem(true);
	}

	private static void addSysItem(StringBuilder buf, String name, String value) {
		buf.append(",{name:" + StringUtil.quote(name) + ",value:"
				+ StringUtil.quote(value) + "}");
	}

	public static void getSysInfo(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		StringBuilder b = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#,##0");
		Runtime rt = Runtime.getRuntime();
		long total = rt.totalMemory(), free = rt.freeMemory();

		b.append("{rows:[{name:\"Total Memory\",value:\""
				+ df.format(total / 1048576d) + " MB\"}");
		addSysItem(b, "Free Memory", df.format(free / 1048576d) + " MB");
		addSysItem(b, "Memory in Use", df.format((total - free) / 1048576d)
				+ " MB");
		addSysItem(b, "Maximum Memory", df.format(rt.maxMemory() / 1048576d)
				+ " MB");
		Properties props = java.lang.System.getProperties();
		addSysItem(b, "System Start Time", DateUtil.formatDate(Main.startTime,
				"yyyy-MM-dd HH:mm:ss"));
		addSysItem(b, "OS Name", props.getProperty("os.name"));
		addSysItem(b, "OS Architecture", props.getProperty("os.arch"));
		addSysItem(b, "OS Version", props.getProperty("os.version"));
		addSysItem(b, "WebBuilder Version", Var.get("webbuilder.version"));
		addSysItem(b, "Java Version", props.getProperty("java.version"));
		addSysItem(b, "Java Vendor", props.getProperty("java.vendor"));
		addSysItem(b, "Work Directory", props.getProperty("user.dir"));
		addSysItem(b, "Extension Directory", props.getProperty("java.ext.dirs"));
		addSysItem(b, "Temporary Directory", props
				.getProperty("java.io.tmpdir"));
		addSysItem(b, "User Name", props.getProperty("user.name"));
		addSysItem(b, "User Home", props.getProperty("user.home"));
		b.append("]}");
		WebUtil.response(response, b);
	}

	public static void updateVar(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Var.set(request.getParameter("fullName"), request
				.getParameter("varValue"));
	}

	private static void addDelVar(PreparedStatement st,
			HashMap<String, VarData> map, Set<Entry<String, VarData>> es,
			String id) throws Exception {
		st.setString(1, id);
		st.addBatch();
		for (Entry<String, VarData> e : es) {
			if (StringUtil.isEqual(e.getValue().parentId, id)) {
				addDelVar(st, map, es, e.getKey());
			}
		}
	}

	public static void deleteVar(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, String> buffer = Var.getVarMap();
		String fullName = request.getParameter("fullName");

		if (fullName.equalsIgnoreCase("server"))
			SysUtil.error(Str.format(request, "cannotDelete", fullName));
		if (StringUtil.substring(fullName, 0, 7).equalsIgnoreCase("server.")) {
			File file = new File(Main.path, "webbuilder/data/config.txt");
			JSONObject jsonObject = JsonUtil.readObject(file);
			jsonObject.remove(fullName.substring(7));
			FileUtil.writeUtfText(file, jsonObject.toString());
			buffer.remove(fullName);
		} else {
			Connection conn = null;
			PreparedStatement st = null;
			String key, varId = request.getParameter("varId");
			int len = fullName.length();
			Set<Entry<String, String>> es = buffer.entrySet();
			HashMap<String, VarData> varBuf = Var.getDbVar();
			Set<Entry<String, VarData>> varEs = varBuf.entrySet();
			ConcurrentHashMap<String, String> map = Var.getDbVarMap();

			try {
				conn = DbUtil.getConnection(request);
				conn.setAutoCommit(false);
				st = conn.prepareStatement("delete from WB_VAR where VAR_ID=?");
				addDelVar(st, varBuf, varEs, varId);
				st.executeBatch();
				for (Entry<String, String> e : es) {
					key = e.getKey();
					if (key.equals(fullName)
							|| (key.length() > len && key.substring(0, len + 1)
									.equals(fullName + "."))) {
						buffer.remove(key);
						map.remove(key);
					}
				}
			} finally {
				DbUtil.closeStatement(st);
			}
		}
	}

	public static void newVar(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ConcurrentHashMap<String, String> buffer = Var.getVarMap();
		String fullName = request.getParameter("fullName"), value = request
				.getParameter("value");

		if (buffer.containsKey(fullName)
				|| StringUtil.isSame(fullName, "server"))
			SysUtil.error(Str.format(request, "alreadyExists", fullName));
		if (StringUtil.substring(fullName, 0, 7).equalsIgnoreCase("server.")) {
			if (!StringUtil.getBool(request.getParameter("isVar")))
				SysUtil.error(Str.format(request, "cannotAppend"));
			File file = new File(Main.path, "webbuilder/data/config.txt");
			JSONObject jsonObject = JsonUtil.readObject(file);
			jsonObject.put(fullName.substring(7), value);
			FileUtil.writeUtfText(file, jsonObject.toString());
		} else {
			DbUtil
					.update(
							request,
							"insert into WB_VAR values({?sys.id?},{?parentId?},{?name?},{?value?},{?integer.isVar?})");
			ConcurrentHashMap<String, String> map = Var.getDbVarMap();
			map.put(fullName, (String) request.getAttribute("sys.id"));
		}
		buffer.put(fullName, value);
		WebUtil.response(response, StringUtil.concat("{id:'", (String) request
				.getAttribute("sys.id"), "'}"));
	}

	public static void getVarTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id, pid = request.getParameter("varId"), key, value;
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;

		if (StringUtil.isEmpty(pid))
			pid = "-1";
		buf.append("{children:[");
		if (StringUtil.isEqual(pid, "-1")) {
			List<Entry<String, ?>> es = StringUtil.sortMapKey(Var
					.getServerVar());
			buf.append("{text:\"server\",varName:\"server\",children:[");
			for (Entry<String, ?> e : es) {
				if (isFirst)
					isFirst = false;
				else
					buf.append(',');
				key = e.getKey();
				buf.append("{text:\"");
				buf.append(key);
				value = (String) e.getValue();
				if (!StringUtil.isEmpty(value)) {
					buf.append("&nbsp;&nbsp;<font color='#808080'>(");
					buf.append(StringUtil.toHTML(value));
					buf.append(")</font>");
				}
				buf
						.append("\",leaf:true,isVar:true,iconCls:\"object_icon\",varValue:");
				buf.append(StringUtil.quote(value));
				buf.append(",varName:\"");
				buf.append(key);
				buf.append("\"}");
			}
			buf.append("]}");
		}
		List<Entry<String, ?>> des = StringUtil.sortMapValue(Var.getDbVar());
		VarData vd;

		for (Entry<String, ?> e : des) {
			vd = (VarData) e.getValue();
			if (StringUtil.isEqual(vd.parentId, pid)) {
				id = e.getKey();
				if (isFirst)
					isFirst = false;
				else
					buf.append(',');
				buf.append("{text:\"");
				buf.append(vd.name);
				value = vd.value;
				if (!StringUtil.isEmpty(value)) {
					buf.append("&nbsp;&nbsp;<font color='#808080'>(");
					buf.append(StringUtil.toHTML(value));
					buf.append(")</font>");
				}
				buf.append("\",varName:\"");
				buf.append(vd.name);
				buf.append("\",leaf:");
				buf.append(vd.isVar);
				if (vd.isVar)
					buf.append(",iconCls:\"object_icon\"");
				else if (!hasSubVar(des, id))
					buf.append(",children:[]");
				buf.append(",varId:\"");
				buf.append(id);
				buf.append("\",varValue:");
				buf.append(StringUtil.quote(value));
				buf.append(",isVar:");
				buf.append(vd.isVar);
				buf.append('}');
			}
		}
		buf.append("]}");
		WebUtil.response(response, buf);
	}

	private static boolean hasSubVar(List<Entry<String, ?>> es, String id) {
		for (Entry<String, ?> e : es) {
			if (StringUtil.isEqual(((VarData) e.getValue()).parentId, id))
				return true;
		}
		return false;
	}
}
