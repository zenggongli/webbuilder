package com.webbuilder.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;

import com.webbuilder.utils.DbUtil;

public class Role {
	private static ConcurrentHashMap<String, String> buffer;

	public static ConcurrentHashMap<String, String> getRoleMap()
			throws Exception {
		if (buffer == null)
			initialize(false);
		return buffer;
	}

	public static synchronized void initialize(boolean reload) throws Exception {
		if (!reload && buffer != null)
			return;
		buffer = new ConcurrentHashMap<String, String>();
		Connection conn = null;
		ResultSet rs = null;

		try {
			conn = DbUtil.getConnection();
			rs = DbUtil.getResultSet(conn, "select * from WB_ROLE");
			while (rs.next()) {
				buffer.put(rs.getString(1), rs.getString(2) + "="
						+ rs.getString(3));
			}
		} finally {
			DbUtil.closeResultSet(rs);
			DbUtil.closeConnection(conn);
		}
	}
}