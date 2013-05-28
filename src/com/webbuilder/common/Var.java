package com.webbuilder.common;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;

public class Var {
	private static ConcurrentHashMap<String, String> buffer;
	private static ConcurrentHashMap<String, String> dbVarMap;

	public static ConcurrentHashMap<String, String> getVarMap()
			throws Exception {
		if (buffer == null)
			initialize(false);
		return buffer;
	}

	public static ConcurrentHashMap<String, String> getDbVarMap()
			throws Exception {
		if (buffer == null)
			initialize(false);
		return dbVarMap;
	}

	public static void set(String name, String value) throws Exception {
		if (StringUtil.isEmpty(name))
			throw new Exception("Name cannot be blank.");
		if (value == null)
			value = "";
		if (buffer == null)
			initialize(false);
		if (StringUtil.substring(name, 0, 7).equalsIgnoreCase("server.")) {
			File file = new File(Main.path, "webbuilder/data/config.txt");
			String key = name.substring(7);
			JSONObject jsonObject = JsonUtil.readObject(file);
			if (!jsonObject.has(key))
				throw new Exception(Str.format("notExist", name));
			jsonObject.put(key, value);
			FileUtil.writeUtfText(file, jsonObject.toString());
		} else {
			String id = dbVarMap.get(name);
			if (StringUtil.isEmpty(id))
				throw new Exception(Str.format("notExist", name));
			Connection conn = null;
			PreparedStatement st = null;
			try {
				conn = DbUtil.getConnection();
				st = conn
						.prepareStatement("update WB_VAR set VAR_VALUE=? where VAR_ID=?");
				st.setString(1, value);
				st.setString(2, id);
				st.executeUpdate();
			} finally {
				DbUtil.closeStatement(st);
				DbUtil.closeConnection(conn);
			}
		}
		buffer.put(name, value);
	}

	public static void set(String name, boolean value) throws Exception {
		set(name, Boolean.toString(value));
	}

	public static void set(String name, int value) throws Exception {
		set(name, Integer.toString(value));
	}

	public static void set(String name, long value) throws Exception {
		set(name, Long.toString(value));
	}

	public static void set(String name, float value) throws Exception {
		set(name, Float.toString(value));
	}

	public static void set(String name, double value) throws Exception {
		set(name, Double.toString(value));
	}

	public static void set(String name, Date date) throws Exception {
		set(name, new Timestamp(date.getTime()).toString());
	}

	public static String get(String name) throws Exception {
		if (buffer == null)
			initialize(false);
		return buffer.get(name);
	}

	public static boolean getBool(String name) throws Exception {
		return StringUtil.getBool(get(name));
	}

	public static boolean getBool(String name, boolean defaultValue) {
		try {
			return StringUtil.getBool(get(name));
		} catch (Throwable e) {
			return defaultValue;
		}
	}

	public static int getInt(String name) throws Exception {
		String k = get(name);
		if (StringUtil.isEmpty(k))
			return 0;
		else
			return Integer.parseInt(k);
	}

	public static long getLong(String name) throws Exception {
		String k = get(name);
		if (StringUtil.isEmpty(k))
			return 0;
		else
			return Long.parseLong(k);
	}

	public static float getFloat(String name) throws Exception {
		String k = get(name);
		if (StringUtil.isEmpty(k))
			return 0;
		else
			return Float.parseFloat(k);
	}

	public static double getDouble(String name) throws Exception {
		String k = get(name);
		if (StringUtil.isEmpty(k))
			return 0;
		else
			return Double.parseDouble(k);
	}

	public static Date getDate(String name) throws Exception {
		String k = get(name);
		if (StringUtil.isEmpty(k))
			return null;
		else
			return Timestamp.valueOf(name);
	}

	public static HashMap<String, String> getServerVar() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		File file = new File(Main.path, "webbuilder/data/config.txt");
		JSONObject jsonObject = JsonUtil.readObject(file);
		String names[] = JSONObject.getNames(jsonObject);
		for (String n : names) {
			map.put(n, jsonObject.optString(n));
		}
		return map;
	}

	public static HashMap<String, VarData> getDbVar() throws Exception {
		HashMap<String, VarData> map = new HashMap<String, VarData>();
		String id;
		VarData vd;
		Connection conn = null;
		ResultSet rs = null;

		try {
			conn = DbUtil.getConnection(buffer.get("server.jndi"));
			rs = DbUtil.getResultSet(conn, "select * from WB_VAR");
			while (rs.next()) {
				vd = new VarData();
				id = rs.getString(1);
				vd.parentId = rs.getString(2);
				vd.name = rs.getString(3);
				vd.value = rs.getString(4);
				if (vd.value == null)
					vd.value = "";
				vd.isVar = rs.getInt(5) == 1;
				map.put(id, vd);
			}
		} finally {
			DbUtil.closeResultSet(rs);
			DbUtil.closeConnection(conn);
		}
		return map;
	}

	public static synchronized void loadServerVar() throws Exception {
		buffer = new ConcurrentHashMap<String, String>();
		putServerVar(getServerVar());
	}

	public static synchronized void initialize(boolean reload) throws Exception {
		if (!reload && buffer != null)
			return;
		buffer = new ConcurrentHashMap<String, String>();
		dbVarMap = new ConcurrentHashMap<String, String>();
		putServerVar(getServerVar());
		putDbVar(getDbVar());
	}

	private static void putServerVar(HashMap<String, String> map) {
		Set<Entry<String, String>> es = map.entrySet();
		for (Entry<String, String> e : es) {
			buffer.put("server." + e.getKey(), e.getValue());
		}
	}

	private static void putDbVar(HashMap<String, VarData> map) {
		String id, name;
		VarData vd;
		Set<Entry<String, VarData>> es = map.entrySet();

		for (Entry<String, VarData> e : es) {
			id = e.getKey();
			vd = e.getValue();
			name = getNameFromId(map, id);
			buffer.put(name, vd.value);
			dbVarMap.put(name, id);
		}
	}

	private static String getNameFromId(HashMap<String, VarData> map,
			String varId) {
		String s = "", id = varId;
		VarData d;

		while ((d = map.get(id)) != null) {
			if (!s.isEmpty())
				s = "." + s;
			s = d.name + s;
			id = d.parentId;
		}
		return s;
	}
}