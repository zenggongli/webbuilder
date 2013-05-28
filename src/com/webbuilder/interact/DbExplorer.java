package com.webbuilder.interact;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Resource;
import com.webbuilder.common.Str;
import com.webbuilder.common.Var;
import com.webbuilder.tool.ExcelObject;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class DbExplorer {
	public static void getProperty(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ResultSet rs = (ResultSet) request.getAttribute("query1");
		ResultSetMetaData meta = rs.getMetaData();
		StringBuilder buf = new StringBuilder();
		int i, j = meta.getColumnCount(), k, scale;

		buf.append("{rows:[");
		for (i = 0; i < j; i++) {
			if (i > 0)
				buf.append(",");
			k = i + 1;
			buf.append("{name:");
			buf.append(StringUtil.quote(meta.getColumnLabel(k)));
			buf.append(",type:");
			buf.append(StringUtil.quote(meta.getColumnTypeName(k)));
			buf.append(",required:");
			buf
					.append(meta.isNullable(k) == ResultSetMetaData.columnNoNulls ? 1
							: 0);
			buf.append(",size:\"");
			buf.append(meta.getPrecision(k));
			scale = meta.getScale(k);
			if (scale != 0) {
				buf.append(",");
				buf.append(scale);
			}
			buf.append("\"}");
		}
		buf.append("]}");
		WebUtil.response(response, buf);
	}

	public static void getBlob(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String table = request.getParameter("___table"), keyField = request
				.getParameter("___field"), value, field;
		Connection conn = null;
		PreparedStatement st2 = null, st3 = null;
		ResultSet rs1 = null, rs2 = null, rs3 = null;
		ResultSetMetaData meta;
		StringBuilder sql = new StringBuilder();
		int i, j, type;
		boolean isFirst = true;
		ArrayList<String> params = new ArrayList<String>();

		try {
			conn = DbUtil.getConnection(request.getParameter("___jndi"));
			rs1 = DbUtil.getResultSet(conn, StringUtil.concat("select * from ",
					table, " where 1=0"));
			meta = rs1.getMetaData();
			sql.append(" from " + table + " where ");
			j = meta.getColumnCount();
			for (i = 0; i < j; i++) {
				type = meta.getColumnType(i + 1);
				if (fieldAsKey(type)) {
					if (isFirst)
						isFirst = false;
					else
						sql.append(" and ");
					field = meta.getColumnLabel(i + 1);
					value = request.getParameter(field);
					if (StringUtil.isEmpty(value)) {
						switch (type) {
						case 1:
						case 12:
						case -9:
						case -15:
							sql.append("(");
							sql.append(field);
							sql.append(" is null or ");
							sql.append(field);
							sql.append("='')");
							break;
						default:
							sql.append(field);
							sql.append(" is null");
						}
					} else {
						sql.append(field);
						sql.append("=?");
						params.add(type + "=" + value);
					}
				}
			}
			st2 = conn.prepareStatement("select count(*) as \"CT\""
					+ sql.toString());
			i = 1;
			for (String t : params) {
				DbUtil.setObject(st2, i++, Integer.parseInt(StringUtil
						.getNamePart(t)), StringUtil.getValuePart(t));
			}
			rs2 = st2.executeQuery();
			if (rs2.next() && rs2.getInt(1) > 1)
				throw new Exception(Str.format(request, "recordNotUnique"));
			st3 = conn.prepareStatement("select " + keyField + sql.toString());
			i = 1;
			for (String t : params) {
				DbUtil.setObject(st3, i++, Integer.parseInt(StringUtil
						.getNamePart(t)), StringUtil.getValuePart(t));
			}
			rs3 = st3.executeQuery();
			DbUtil.outputBlob(rs3, request, response, true);
		} finally {
			DbUtil.closeResultSet(rs1);
			DbUtil.closeResultSet(rs2);
			DbUtil.closeResultSet(rs3);
			DbUtil.closeStatement(st2);
			DbUtil.closeStatement(st3);
			DbUtil.closeConnection(conn);
		}
	}

	public static void exportData(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.reset();
		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;"
				+ WebUtil.encodeFilename(request, request.getParameter("title")
						+ ".txt"));
		Writer writer = new BufferedWriter(new OutputStreamWriter(response
				.getOutputStream(), "utf-8"));
		try {
			DbUtil.exportData(writer, (ResultSet) request
					.getAttribute("query1"));
		} finally {
			writer.close();
		}
		response.flushBuffer();
	}

	public static void importData(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String filename = (String) request.getAttribute("uploadFile__name");
		if (FileUtil.extractFileExt(filename).equalsIgnoreCase("xls"))
			importExcel(request, response);
		else
			importTxt(request, response);
	}

	private static void importTxt(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(StringUtil
				.getUtfString((InputStream) request.getAttribute("uploadFile")));
		Connection conn = DbUtil.getConnection(request, (String) request
				.getAttribute("jndi"));
		conn.setAutoCommit(false);
		String table = (String) request.getAttribute("table");
		DbUtil.importData(conn, table, ja);
	}

	private static void importExcel(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ResultSet rs = null;
		PreparedStatement st = null;
		Connection conn = null;
		HSSFWorkbook wb;
		HSSFSheet sheet;
		HSSFRow row;
		HSSFCell cell;
		Object obj;
		Iterator<?> rows, cells;
		ResultSetMetaData meta;
		boolean firstRow = true, isDate[] = null;
		int i, j, types[] = null;
		String val, table;
		StringBuilder fields = new StringBuilder();

		try {
			wb = new HSSFWorkbook((InputStream) request
					.getAttribute("uploadFile"));
			sheet = wb.getSheetAt(0);
			rows = sheet.rowIterator();
			conn = DbUtil.getConnection((String) request.getAttribute("jndi"));
			conn.setAutoCommit(false);
			table = (String) request.getAttribute("table");
			while (rows.hasNext()) {
				row = (HSSFRow) rows.next();
				cells = row.cellIterator();
				i = 0;
				while (cells.hasNext()) {
					cell = (HSSFCell) cells.next();
					if (firstRow) {
						val = cell.getStringCellValue();
						if (i > 0)
							fields.append(",");
						fields.append(val);
					} else {
						obj = ExcelObject.getCellValue(cell);
						if (isDate[i] && obj instanceof Double)
							obj = HSSFDateUtil.getJavaDate((Double) obj);
						DbUtil.setObject(st, i + 1, types[i], obj);
					}
					i++;
				}
				if (firstRow) {
					firstRow = false;
					rs = DbUtil.getResultSet(conn, StringUtil.concat("select ",
							fields.toString(), " from ", table, " where 1=0"));
					meta = rs.getMetaData();
					j = meta.getColumnCount();
					types = new int[j];
					isDate = new boolean[j];
					st = conn.prepareStatement(StringUtil.concat(
							"insert into ", table, " (", fields.toString(),
							") values (", StringUtil.duplicate(",?", j)
									.substring(1), ")"));
					for (i = 0; i < j; i++) {
						types[i] = meta.getColumnType(i + 1);
						isDate[i] = DbUtil.getTypeCategory(types[i]).equals(
								"date");
					}
				} else
					st.addBatch();
			}
			st.executeBatch();
			conn.commit();
		} finally {
			DbUtil.closeResultSet(rs);
			DbUtil.closeStatement(st);
			DbUtil.closeConnection(conn);
		}
	}

	private static boolean fieldAsKey(int type) throws Exception {
		boolean f = Var.getBool("webbuilder.app.dbe.floatFieldAsKey");
		boolean b = Var.getBool("webbuilder.app.dbe.bigFieldAsKey");
		switch (type) {
		case Types.REAL:
		case Types.FLOAT:
		case Types.DOUBLE:
			return f;
		case Types.LONGVARCHAR:
		case Types.LONGNVARCHAR:
		case Types.CLOB:
		case Types.NCLOB:
			return b;
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			return false;
		}
		return true;
	}

	public static void deleteData(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(request.getParameter("data"));
		int i, j = ja.length(), k, x, y, type, types[];
		if (j == 0)
			return;
		boolean isFirst;
		String table = request.getParameter("table"), val, params[], deleteSql;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement delete = null;
		ResultSetMetaData meta;
		StringBuilder buf = new StringBuilder();
		JSONObject jo;

		try {
			conn = DbUtil.getConnection(request.getParameter("jndi"));
			conn.setAutoCommit(false);
			rs = DbUtil.getResultSet(conn, StringUtil.concat("select * from ",
					table, " where 1=0"));
			meta = rs.getMetaData();
			y = meta.getColumnCount();
			isFirst = true;
			params = new String[y];
			types = new int[y];
			deleteSql = "delete from " + table + " where ";

			for (x = 0; x < y; x++) {
				type = meta.getColumnType(x + 1);
				types[x] = type;
				if (fieldAsKey(type))
					params[x] = meta.getColumnName(x + 1);
			}
			for (i = 0; i < j; i++) {
				jo = ja.getJSONObject(i);
				isFirst = true;
				buf.delete(0, buf.length());
				for (x = 0; x < y; x++) {
					if (fieldAsKey(types[x])) {
						if (isFirst)
							isFirst = false;
						else
							buf.append(" and ");
						if (JsonUtil.optString(jo, params[x]).isEmpty()) {
							switch (types[x]) {
							case 1:
							case 12:
							case -9:
							case -15:
								buf.append("(");
								buf.append(params[x]);
								buf.append(" is null or ");
								buf.append(params[x]);
								buf.append("='')");
								break;
							default:
								buf.append(params[x]);
								buf.append(" is null");
							}
						} else {
							buf.append(params[x]);
							buf.append("=?");
						}
					}
				}
				delete = conn.prepareStatement(StringUtil.concat(deleteSql, buf
						.toString()));
				k = 1;
				for (x = 0; x < y; x++) {
					val = JsonUtil.optString(jo, params[x]);
					if (!val.isEmpty())
						DbUtil.setObject(delete, k++, types[x], val);
				}
				k = delete.executeUpdate();
				if (k > 1)
					throw new Exception(Str.format(request, "recordNotUnique"));
				else if (k == 0)
					throw new Exception("Record cannot be deleted.");
				delete.close();
			}
			conn.commit();
		} finally {
			DbUtil.closeResultSet(rs);
			DbUtil.closeStatement(delete);
			DbUtil.closeConnection(conn);
		}
	}

	public static void saveData(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(request.getParameter("data"));
		int i, j = ja.length(), k, x, y, z, type, types[];
		if (j == 0)
			return;
		boolean isFirst;
		String table = request.getParameter("table"), val, name, params[], updateSql;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement insert = null, update = null;
		ResultSetMetaData meta;
		StringBuilder buf = new StringBuilder(), fields = new StringBuilder();
		JSONObject jo, origin;

		try {
			conn = DbUtil.getConnection(request.getParameter("jndi"));
			conn.setAutoCommit(false);
			rs = DbUtil.getResultSet(conn, StringUtil.concat("select * from ",
					table, " where 1=0"));
			meta = rs.getMetaData();
			y = meta.getColumnCount();
			isFirst = true;
			z = 0;
			params = new String[y];
			types = new int[y];
			buf.append("update ");
			buf.append(table);
			buf.append(" set ");
			for (x = 0; x < y; x++) {
				type = meta.getColumnType(x + 1);
				if (!DbUtil.isBlobField(type) && !meta.isReadOnly(x + 1)) {
					if (isFirst)
						isFirst = false;
					else {
						fields.append(",");
						buf.append(",");
					}
					name = meta.getColumnName(x + 1);
					fields.append(name);
					buf.append(name);
					buf.append("=?");
					types[z] = type;
					params[z] = name;
					z++;
				}
			}
			insert = conn.prepareStatement(StringUtil.concat("insert into ",
					table, "(", fields.toString(), ") values (", StringUtil
							.duplicate(",?", z).substring(1), ")"));
			buf.append(" where ");
			updateSql = buf.toString();
			for (i = 0; i < j; i++) {
				jo = ja.getJSONObject(i);
				if (jo.has("__isNew")) {
					for (x = 0; x < z; x++) {
						DbUtil.setObject(insert, x + 1, types[x], JsonUtil.opt(
								jo, params[x]));
					}
					insert.executeUpdate();
				} else {
					isFirst = true;
					origin = jo.getJSONObject("__origin");
					buf.delete(0, buf.length());
					for (x = 0; x < z; x++) {
						if (fieldAsKey(types[x])) {
							if (isFirst)
								isFirst = false;
							else
								buf.append(" and ");
							if (JsonUtil.optString(origin, params[x]).isEmpty()) {
								switch (types[x]) {
								case 1:
								case 12:
								case -9:
								case -15:
									buf.append("(");
									buf.append(params[x]);
									buf.append(" is null or ");
									buf.append(params[x]);
									buf.append("='')");
									break;
								default:
									buf.append(params[x]);
									buf.append(" is null");
								}
							} else {
								buf.append(params[x]);
								buf.append("=?");
							}
						}
					}
					update = conn.prepareStatement(StringUtil.concat(updateSql,
							buf.toString()));
					for (x = 0; x < z; x++)
						DbUtil.setObject(update, x + 1, types[x], JsonUtil.opt(
								jo, params[x]));
					k = 0;
					for (x = 0; x < z; x++) {
						val = JsonUtil.optString(origin, params[x]);
						if (fieldAsKey(types[x]) && !val.isEmpty())
							DbUtil
									.setObject(update, z + 1 + k++, types[x],
											val);
					}
					k = update.executeUpdate();
					if (k != 1)
						throw new Exception(Str.format(request,
								"recordNotUnique"));
					update.close();
				}
			}
			conn.commit();
		} finally {
			DbUtil.closeResultSet(rs);
			DbUtil.closeStatement(insert);
			DbUtil.closeStatement(update);
			DbUtil.closeConnection(conn);
		}
	}

	public static void removeJndi(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String tree = Resource.get("wb.dbe.tree");
		if (!StringUtil.isEmpty(tree)) {
			String id = request.getParameter("id");
			JSONArray ja = new JSONArray(tree);
			JSONObject jo;
			boolean deleted = false;

			int i, j = ja.length();
			for (i = 0; i < j; i++) {
				jo = ja.getJSONObject(i);
				if (jo.optString("id").equals(id)) {
					ja.remove(i);
					deleted = true;
					break;
				}
			}
			if (deleted)
				Resource.set("wb.dbe.tree", ja.toString());
		}
	}

	public static void newJndi(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String jndi = request.getParameter("jndiEdit"), text = request
				.getParameter("dispEdit");
		DbUtil.testJndi(jndi);
		String tree = Resource.get("wb.dbe.tree");
		JSONArray ja;
		if (StringUtil.isEmpty(tree))
			ja = new JSONArray();
		else {
			ja = new JSONArray(tree);
			if (JsonUtil.findObject(ja, "text", text) != null)
				throw new Exception(Str.format(request, "alreadyExists", text));
		}
		JSONObject jo = new JSONObject();
		String result, index = request.getParameter("index");

		jo.put("text", text);
		jo.put("jndi", jndi);
		jo.put("id", (String) request.getAttribute("sys.id"));
		if (StringUtil.isEmpty(index)) {
			ja.put(jo);
			result = ja.toString();
		} else {
			int ix = Integer.parseInt(index) - 1;
			if (ix == -1)
				ix = 0;
			result = JsonUtil.insert(ja, jo.toString(), ix);
		}
		Resource.set("wb.dbe.tree", result);
		WebUtil.response(response, jo);
	}

	public static void getList(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String jndi = request.getParameter("jndi");

		if (StringUtil.isEmpty(jndi))
			WebUtil.response(response, getDbList(request));
		else
			WebUtil.response(response, getTableList(jndi));
	}

	private static String getTableList(String jndi) throws Exception {
		Connection conn = null;
		ResultSet rs = null;
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;
		String[] types = { "TABLE" }, tables;
		ArrayList<String> tableList = new ArrayList<String>();

		try {
			conn = DbUtil.getConnection(jndi);
			rs = conn.getMetaData().getTables(null, null, null, types);
			while (rs.next()) {
				tableList.add(rs.getString("TABLE_NAME"));
			}
			tables = StringUtil.sort(tableList);
			buf.append('[');
			for (String t : tables) {
				if (isFirst)
					isFirst = false;
				else
					buf.append(',');
				buf.append("{text:\"");
				buf.append(t);
				buf.append("\",leaf:true,iconCls:\"table_icon\"}");
			}
			buf.append(']');
			return buf.toString();
		} finally {
			DbUtil.closeResultSet(rs);
			DbUtil.closeConnection(conn);
		}
	}

	private static String getDbList(HttpServletRequest request)
			throws Exception {
		JSONObject jo;
		JSONArray ja = new JSONArray(
				"[{text:\"{#Str.defaultStr#}\",jndi:\"{#Var.server.jndi#}\",id:\"default\"}]");
		String tree = Resource.get("wb.dbe.tree");
		int i, j;
		if (tree != null) {
			JSONArray treeJa = new JSONArray(tree);
			j = treeJa.length();
			for (i = 0; i < j; i++)
				ja.put(treeJa.get(i));
		}
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;

		j = ja.length();
		buf.append('[');
		for (i = 0; i < j; i++) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(',');
			jo = ja.getJSONObject(i);
			jo.put("iconCls", "db_icon");
			buf.append(StringUtil.replaceParameters(request, jo.toString()));
		}
		buf.append(']');
		return buf.toString();
	}
}
