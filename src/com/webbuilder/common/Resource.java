package com.webbuilder.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;

import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.SysUtil;
import com.webbuilder.utils.WebUtil;

public class Resource {
	public static String get(HttpServletRequest request, String id)
			throws Exception {
		return get(WebUtil.getIdWithUser(request, id));
	}

	public static String get(String id) throws Exception {
		byte[] bytes = getBytes(id);
		if (bytes == null)
			return null;
		return new String(bytes, "utf-8");
	}

	public static byte[] getBytes(HttpServletRequest request, String id)
			throws Exception {
		return getBytes(WebUtil.getIdWithUser(request, id));
	}

	public static byte[] getBytes(String id) throws Exception {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			conn = DbUtil.getConnection();
			st = conn
					.prepareStatement("select RES_CONTENT from WB_RESOURCE where RES_ID=?");
			st.setString(1, id);
			rs = st.executeQuery();
			if (rs.next()) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				InputStream is = rs.getBinaryStream(1);
				if (is != null) {
					try {
						SysUtil.isToOs(is, os);
					} finally {
						is.close();
					}
					return os.toByteArray();
				}
			}
		} finally {
			if (rs == null)
				DbUtil.closeStatement(st);
			else
				DbUtil.closeResultSet(rs);
			DbUtil.closeConnection(conn);
		}
		return null;
	}

	public static void set(HttpServletRequest request, String id, String data)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), data);
	}

	public static void set(String id, String data) throws Exception {
		byte[] bytes;

		if (data == null)
			bytes = null;
		else
			bytes = data.getBytes("utf-8");
		set(id, bytes);
	}

	public static void set(HttpServletRequest request, String id, byte[] data)
			throws Exception {
		set(WebUtil.getIdWithUser(request, id), data);
	}

	public static void remove(String id) throws Exception {
		set(id, (byte[]) null);
	}

	public static void set(String id, byte[] data) throws Exception {
		Connection conn = null;
		PreparedStatement st = null;

		conn = DbUtil.getConnection();
		try {
			conn.setAutoCommit(false);
			st = conn
					.prepareStatement("delete from WB_RESOURCE where RES_ID=?");
			st.setString(1, id);
			st.executeUpdate();
			DbUtil.closeStatement(st);
			st = null;
			if (data != null) {
				st = conn
						.prepareStatement("insert into WB_RESOURCE values(?,?)");
				st.setString(1, id);
				st.setBinaryStream(2, new ByteArrayInputStream(data),
						data.length);
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
