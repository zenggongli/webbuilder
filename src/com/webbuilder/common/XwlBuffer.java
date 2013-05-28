package com.webbuilder.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.StringUtil;

public class XwlBuffer {
	private static ConcurrentHashMap<String, JSONObject> metaMap;
	private static ConcurrentHashMap<String, XwlData> xwlMap;

	public static XwlData getXwl(String id) throws Exception {
		if (xwlMap == null)
			initialize(false);
		return xwlMap.get(id);
	}

	public static JSONObject getMeta(String name) throws Exception {
		if (xwlMap == null)
			initialize(false);
		return metaMap.get(name);
	}

	public static ConcurrentHashMap<String, XwlData> getXwlMap()
			throws Exception {
		if (xwlMap == null)
			initialize(false);
		return xwlMap;
	}

	public static ConcurrentHashMap<String, JSONObject> getMetaMap()
			throws Exception {
		if (xwlMap == null)
			initialize(false);
		return metaMap;
	}

	public static boolean exists(String id) throws Exception {
		if (xwlMap == null)
			initialize(false);
		return StringUtil.isEqual(id, "-1") || xwlMap.containsKey(id);
	}

	private static void loadMeta(Connection conn) throws Exception {
		PreparedStatement stmt = conn
				.prepareStatement("select META_NAME,META_CONTENT from WB_META");
		ResultSet rs = stmt.executeQuery();
		while (rs.next())
			metaMap.put(rs.getString(1), new JSONObject(DbUtil.getCharStream(
					rs, 2)));
		rs.close();
		stmt.close();
	}

	private static HashMap<String, ArrayList<String>> getRolesMap(
			Connection conn) throws Exception {
		String id, oldId = "";
		HashMap<String, ArrayList<String>> roleMap;
		ArrayList<String> roles = null;
		ResultSet rs = DbUtil
				.getResultSet(
						conn,
						"select a.MODULE_ID,a.ROLE_ID,b.ROLE_NAME from WB_MODULE_ROLE a, WB_ROLE b where a.ROLE_ID=b.ROLE_ID order by a.MODULE_ID");
		try {
			roleMap = new HashMap<String, ArrayList<String>>();
			while (rs.next()) {
				id = rs.getString(1);
				if (!oldId.equals(id)) {
					if (roles != null)
						roleMap.put(oldId, roles);
					roles = new ArrayList<String>();
				}
				roles.add(rs.getString(2) + "=" + rs.getString(3));
				oldId = id;
			}
			if (roles != null)
				roleMap.put(oldId, roles);
		} finally {
			DbUtil.closeResultSet(rs);
		}
		return roleMap;
	}

	private static void loadXwl(Connection conn) throws Exception {
		ResultSet module = null;
		XwlData data;
		String id, content;
		HashMap<String, ArrayList<String>> roles;

		try {
			module = DbUtil.getResultSet(conn, "select * from WB_MODULE");
			roles = getRolesMap(conn);
			while (module.next()) {
				data = new XwlData();
				id = module.getString(1);
				data.parentId = module.getString(2);
				content = DbUtil.getCharStream(module, 3);
				data.isFolder = StringUtil.isEmpty(content);
				if (!data.isFolder)
					data.content = new JSONObject(content);
				data.title = module.getString(4);
				data.icon = module.getString(5);
				if (data.icon == null)
					data.icon = "";
				data.isHidden = StringUtil.getBool(module.getString(6));
				data.newWin = StringUtil.getBool(module.getString(7));
				data.createUser = module.getString(8);
				data.createDate = module.getTimestamp(9);
				data.lastModifyUser = module.getString(10);
				data.lastModifyDate = module.getTimestamp(11);
				data.orderIndex = module.getInt(12);
				data.roles = roles.get(id);
				xwlMap.put(id, data);
			}
		} finally {
			DbUtil.closeResultSet(module);
		}
	}

	public static synchronized void initialize(boolean reload) throws Exception {
		if (!reload && xwlMap != null)
			return;
		Connection conn = DbUtil.getConnection();
		try {
			metaMap = new ConcurrentHashMap<String, JSONObject>();
			loadMeta(conn);
			xwlMap = new ConcurrentHashMap<String, XwlData>();
			loadXwl(conn);
		} finally {
			conn.close();
		}
	}
}