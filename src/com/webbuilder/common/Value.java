package com.webbuilder.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Value {
	public static Integer getInt(String id) throws Exception {
		String v = get(id);
		if (StringUtil.isEmpty(v))
			return null;
		return Integer.parseInt(v);
	}

	public static Integer getInt(HttpServletRequest request, String id)
			throws Exception {
		return getInt(WebUtil.getIdWithUser(request, id));
	}

	public static Long getLong(String id) throws Exception {
		String v = get(id);
		if (StringUtil.isEmpty(v))
			return null;
		return Long.parseLong(v);
	}

	public static Long getLong(HttpServletRequest request, String id)
			throws Exception {
		return getLong(WebUtil.getIdWithUser(request, id));
	}

	public static Float getFloat(String id) throws Exception {
		String v = get(id);
		if (StringUtil.isEmpty(v))
			return null;
		return Float.parseFloat(v);
	}

	public static Float getFloat(HttpServletRequest request, String id)
			throws Exception {
		return getFloat(WebUtil.getIdWithUser(request, id));
	}

	public static Double getDouble(String id) throws Exception {
		String v = get(id);
		if (StringUtil.isEmpty(v))
			return null;
		return Double.parseDouble(v);
	}

	public static Double getDouble(HttpServletRequest request, String id)
			throws Exception {
		return getDouble(WebUtil.getIdWithUser(request, id));
	}

	public static Boolean getBool(String id) throws Exception {
		String v = get(id);
		if (StringUtil.isEmpty(v))
			return null;
		return Boolean.parseBoolean(v);
	}

	public static Boolean getBool(HttpServletRequest request, String id)
			throws Exception {
		return getBool(WebUtil.getIdWithUser(request, id));
	}

	public static Date getDate(String id) throws Exception {
		String v = get(id);
		if (StringUtil.isEmpty(v))
			return null;
		return Timestamp.valueOf(v);
	}

	public static Date getDate(HttpServletRequest request, String id)
			throws Exception {
		return getDate(WebUtil.getIdWithUser(request, id));
	}

	public static String get(HttpServletRequest request, String id)
			throws Exception {
		return get(WebUtil.getIdWithUser(request, id));
	}

	public static String get(String id) throws Exception {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			conn = DbUtil.getConnection();
			st = conn
					.prepareStatement("select VAL_CONTENT from WB_VALUE where VAL_ID=?");
			st.setString(1, id);
			rs = st.executeQuery();
			if (rs.next())
				return rs.getString(1);
		} finally {
			if (rs == null)
				DbUtil.closeStatement(st);
			else
				DbUtil.closeResultSet(rs);
			DbUtil.closeConnection(conn);
		}
		return null;
	}

	public static void set(String id, Integer value) throws Exception {
		String v;
		if (value == null)
			v = null;
		else
			v = Integer.toString(value);
		set(id, v);
	}

	public static void set(HttpServletRequest request, String id, Integer value)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, Float value) throws Exception {
		String v;
		if (value == null)
			v = null;
		else
			v = Float.toString(value);
		set(id, v);
	}

	public static void set(HttpServletRequest request, String id, Float value)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, Long value) throws Exception {
		String v;
		if (value == null)
			v = null;
		else
			v = Long.toString(value);
		set(id, v);
	}

	public static void set(HttpServletRequest request, String id, Long value)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, Double value) throws Exception {
		String v;
		if (value == null)
			v = null;
		else
			v = Double.toString(value);
		set(id, v);
	}

	public static void set(HttpServletRequest request, String id, Double value)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, Boolean value) throws Exception {
		String v;
		if (value == null)
			v = null;
		else
			v = Boolean.toString(value);
		set(id, v);
	}

	public static void set(HttpServletRequest request, String id, Boolean value)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, Date value) throws Exception {
		String v;
		if (value == null)
			v = null;
		else
			v = DateUtil.toString(value);
		set(id, v);
	}

	public static void set(HttpServletRequest request, String id, Date value)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(HttpServletRequest request, String id, String value)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void remove(String id) throws Exception {
		set(id, (String) null);
	}

	public static void set(String id, String value) throws Exception {
		Connection conn = null;
		PreparedStatement st = null;

		conn = DbUtil.getConnection();
		try {
			conn.setAutoCommit(false);
			st = conn.prepareStatement("delete from WB_VALUE where VAL_ID=?");
			st.setString(1, id);
			st.executeUpdate();
			DbUtil.closeStatement(st);
			st = null;
			if (value != null) {
				st = conn.prepareStatement("insert into WB_VALUE values(?,?)");
				st.setString(1, id);
				st.setString(2, value);
				st.executeUpdate();
			}
			conn.commit();
		} catch (Throwable e) {
			conn.rollback();
			throw new Exception(e);
		} finally {
			DbUtil.closeStatement(st);
			DbUtil.closeConnection(conn);
		}
	}
}
