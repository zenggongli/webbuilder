package com.webbuilder.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.StringUtil;

public class SysMap {
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, String>> buffer;

	public static String get(String type, String name) throws Exception {
		if (buffer == null)
			initialize(false);
		ConcurrentHashMap<String, String> map = buffer.get(type);
		if (map == null)
			return null;
		else
			return map.get(name);
	}

	public static void put(String type, String name, String value)
			throws Exception {
		if (buffer == null)
			initialize(false);
		ConcurrentHashMap<String, String> map = buffer.get(type);
		if (map == null)
			map = new ConcurrentHashMap<String, String>();
		map.put(name, value);
		buffer.put(type, map);
	}

	public static void remove(String type, String name) throws Exception {
		if (buffer == null)
			return;
		ConcurrentHashMap<String, String> map = buffer.get(type);
		if (map != null) {
			map.remove(name);
			if (map.isEmpty())
				buffer.remove(type);
		}
	}

	public static String getList(String type, String sortType) {
		ConcurrentHashMap<String, String> map = buffer.get(type);
		if (map == null)
			return "[]";
		List<Entry<String, ?>> ls;
		if (StringUtil.isSame(sortType, "keyAsNumber"))
			ls = StringUtil.sortMapKey(map, true);
		else if (StringUtil.isSame(sortType, "value"))
			ls = StringUtil.sortMapValue(map);
		else
			ls = StringUtil.sortMapKey(map);
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;

		buf.append("[");
		for (Entry<String, ?> e : ls) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(",");
			buf.append("[");
			buf.append(StringUtil.quote(e.getKey()));
			buf.append(",");
			buf.append(StringUtil.quote(e.getValue().toString()));
			buf.append("]");
		}
		buf.append("]");
		return buf.toString();
	}

	private static void loadKeys() throws Exception {
		Connection conn = null;
		ResultSet rs = null;
		String type = null, preType = null;
		ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();

		try {
			conn = DbUtil.getConnection();
			rs = DbUtil
					.getResultSet(conn,
							"select KEY_TYPE,KEY_NAME,KEY_VALUE from WB_KEY order by KEY_TYPE");
			while (rs.next()) {
				type = rs.getString(1);
				if (preType != null && !preType.equals(type)) {
					buffer.put(preType, map);
					map = new ConcurrentHashMap<String, String>();
				}
				map.put(rs.getString(2), rs.getString(3));
				preType = type;
			}
			if (preType != null)
				buffer.put(preType, map);
		} finally {
			DbUtil.closeResultSet(rs);
			DbUtil.closeConnection(conn);
		}
	}

	public static synchronized void initialize(boolean reload) throws Exception {
		if (!reload && buffer != null)
			return;
		buffer = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
		loadKeys();
	}
}