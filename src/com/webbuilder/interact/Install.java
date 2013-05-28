package com.webbuilder.interact;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.webbuilder.common.Main;
import com.webbuilder.common.Str;
import com.webbuilder.common.Var;
import com.webbuilder.tool.Encrypter;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.SysUtil;
import com.webbuilder.utils.WebUtil;
import com.webbuilder.utils.ZipUtil;

public class Install {
	private static boolean isRespUpdate = false;

	public static void respUpdate(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ResultSet rs = (ResultSet) request.getAttribute("query2");
		WebUtil.response(response, rs.next() ? "ok" : "failed");
	}

	public static void checkUpdate(String contextPath) {
		final String ctx = contextPath;
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					JSONObject params = new JSONObject();
					params.put("xwlTitle", Var.get("webbuilder.title"));
					params.put("xwlRegCode", Var.get("server.serialNumber"));
					params.put("xwlContext", ctx);
					String r = WebUtil
							.request(
									"http://www.putdb.com/main?xwl=checkUpdate",
									params);
					isRespUpdate = StringUtil.isSame(r, "failed");
				} catch (Throwable e) {
				}
			}
		});
		thread.start();
	}

	public static boolean checkInstall() {
		try {
			HashMap<String, String> map = Var.getServerVar();
			String jndi = map.get("jndi");
			if (StringUtil.isEmpty(jndi))
				return false;
		} catch (Throwable e) {
		}
		return true;
	}

	public static void setup(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String xwl = request.getParameter("xwl");
		if (StringUtil.isEqual(xwl, "xwlfist")) {
			try {
				String jndi = request.getParameter("jndi"), dbType = request
						.getParameter("dbType");
				DbUtil.testJndi(jndi);
				doInstall(jndi, dbType);
			} catch (Throwable e) {
				response
						.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				WebUtil.response(response, StringUtil.toHTML(SysUtil
						.getShortError(e)));
			}
		} else
			WebUtil.response(response, FileUtil.readUtfText(new File(Main.path,
					"webbuilder/data/setup.txt")));
	}

	public static void exportTables(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String tables[] = { "WB_CUST", "WB_DUAL", "WB_KEY", "WB_META",
				"WB_MODULE", "WB_MODULE_ROLE", "WB_ROLE", "WB_TASK", "WB_USER",
				"WB_USER_ROLE", "WB_VAR" };
		File fs[] = (new File(Main.path, "webbuilder/data/table")).listFiles();
		BufferedWriter writer = null;
		for (File f : fs)
			f.delete();
		Connection conn = DbUtil.getConnection(request);
		ResultSet rs;

		for (String t : tables) {
			rs = DbUtil.getResultSet(conn, "select * from " + t);
			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(new File(Main.path, StringUtil
								.concat("webbuilder/data/table/", t, ".txt"))),
						"utf-8"));
				DbUtil.exportData(writer, rs);
			} finally {
				DbUtil.closeResultSet(rs);
				if (writer != null)
					writer.close();
			}
		}
	}

	public static void exportPackage(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.reset();
		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;"
				+ WebUtil.encodeFilename(request, "webbuilder.zip"));
		ZipUtil.zip(Main.path.listFiles(), response.getOutputStream());
		response.flushBuffer();
	}

	public static void doInstall(String jndi, String dbType) throws Exception {
		File file = new File(Main.path, StringUtil.concat(
				"webbuilder/data/sql/", dbType, ".txt"));
		Connection conn = null;
		Statement st = null;
		try {
			int index;
			String table, sqls[] = FileUtil.readUtfText(file).split(";");
			conn = DbUtil.getConnection(jndi);
			st = conn.createStatement();
			for (String t : sqls) {
				index = t.indexOf("CREATE TABLE");
				if (index == -1)
					st.executeUpdate(t.trim());
				else {
					table = t.substring(index + 13, t.indexOf('(')).trim();
					try {
						st.executeUpdate("DROP TABLE " + table);
					} catch (Throwable e) {
					}
					st.executeUpdate(t.trim());
					file = new File(Main.path, StringUtil.concat(
							"webbuilder/data/table/", table, ".txt"));
					if (file.exists())
						DbUtil
								.importData(conn, table, JsonUtil
										.readArray(file));
				}
			}
		} finally {
			DbUtil.closeConnection(conn);
			DbUtil.closeStatement(st);
		}
		Main.installed = true;
		Var.set("server.jndi", jndi);
		Var.set("server.dbType", dbType);
		MngTool.loadSystem(true);
	}

	public static void register(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Var.set("server.serialNumber", request.getParameter("serialNumber"));
		if (!isRegistered())
			throw new Exception(Str.format(request, "invalidSN"));
	}

	public static int getUserCount() throws Exception {
		String s = Var.get("server.serialNumber");
		int users[] = { 10, 100, 1000, 10000, 100000, -1 };
		if (isRegistered() && !isRespUpdate) {
			return users[Integer.parseInt(s.substring(5, 6)) - 1];
		} else
			return 3;
	}
	public static boolean isRegistered() throws Exception {
		return true;
	}
	public static boolean isRegistered2() throws Exception {
		String s = Var.get("server.serialNumber"), v;

		if (s == null || s.length() != 19)
			return false;
		v = s.substring(15);
		s = s.substring(5, 9) + s.substring(10, 14) + s.substring(0, 4);
		int i, j = 0;

		for (i = 0; i < 12; i++) {
			j += s.charAt(i);
			j = j * (i + 2) * 2 + i;
			if (j % 2 == 0)
				j += 1;
			else
				j += 2;
		}
		return v.equals(Encrypter.getMD5(s + Integer.toString(j)).substring(0,
				4));
	}
}