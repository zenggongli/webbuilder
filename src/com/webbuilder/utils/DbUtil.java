package com.webbuilder.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Main;
import com.webbuilder.common.Str;
import com.webbuilder.common.Var;
import com.webbuilder.controls.Query;

public class DbUtil {
	public static void testJndi(String jndi) throws Exception {
		Connection conn = getConnection(jndi);
		conn.close();
	}

	public static void importData(Connection conn, String tableName,
			JSONArray ja) throws Exception {
		ResultSet rs = null;
		PreparedStatement st = null;
		StringBuilder buf = new StringBuilder();
		String fields;
		JSONArray rec;
		int i, j, types[], x, y = ja.length();

		try {
			if (y < 2)
				return;
			rec = ja.getJSONArray(0);
			j = rec.length();
			for (i = 0; i < j; i++) {
				if (i > 0)
					buf.append(",");
				buf.append(rec.getString(i));
			}
			fields = buf.toString();
			rs = getResultSet(conn, StringUtil.concat("select ", fields,
					" from ", tableName, " where 1=0"));
			ResultSetMetaData meta = rs.getMetaData();
			types = new int[j];
			for (i = 0; i < j; i++)
				types[i] = meta.getColumnType(i + 1);
			st = conn.prepareStatement(StringUtil.concat("insert into ",
					tableName, "(", fields, ") values (", StringUtil.duplicate(
							",?", j).substring(1), ")"));
			for (x = 1; x < y; x++) {
				rec = ja.getJSONArray(x);
				for (i = 0; i < j; i++)
					setObject(st, i + 1, types[i], JsonUtil.opt(rec, i));
				st.addBatch();
			}
			st.executeBatch();
		} finally {
			closeResultSet(rs);
			closeStatement(st);
		}
	}

	public static Connection getConnection() throws Exception {
		return getConnection("");
	}

	public static Connection getConnection(HttpServletRequest request)
			throws Exception {
		return getConnection(request, null);
	}

	public static Connection getConnection(String jndi) throws Exception {
		if (StringUtil.isEmpty(jndi))
			jndi = Var.get("server.jndi");
		InitialContext ctx = new InitialContext();
		DataSource ds = (DataSource) ctx.lookup(jndi);
		return ds.getConnection();
	}

	public static Connection getConnection(HttpServletRequest request,
			String jndi) throws Exception {
		Connection conn;
		String jn, storeName;
		Object obj;

		if (StringUtil.isEmpty(jndi))
			jn = Var.get("server.jndi");
		else
			jn = jndi;
		storeName = "jndi@@" + jn;
		obj = request.getAttribute(storeName);
		if (obj == null) {
			conn = getConnection(jn);
			request.setAttribute(storeName, conn);
		} else
			conn = (Connection) obj;
		return conn;
	}

	public static void closeConnection(Connection connection) {
		closeConnection(connection, true);
	}

	public static void closeConnection(Connection connection, boolean rollBack) {
		if (connection == null)
			return;
		try {
			if (connection.isClosed())
				return;
			try {
				if (!connection.getAutoCommit())
					if (rollBack)
						connection.rollback();
					else
						connection.commit();
			} catch (Throwable e) {
				if (!rollBack)
					connection.rollback();
			} finally {
				connection.close();
				connection = null;
			}
		} catch (Throwable e) {
		}
	}

	public static ResultSet getResultSet(Connection conn, String sql)
			throws Exception {
		PreparedStatement st = conn.prepareStatement(sql);
		try {
			return st.executeQuery();
		} catch (Throwable e) {
			closeStatement(st);
			throw new Exception(e);
		}
	}

	public static void closeResultSet(ResultSet resultSet) {
		if (resultSet == null)
			return;
		Statement stm = null;
		try {
			stm = resultSet.getStatement();
		} catch (Throwable e) {
		} finally {
			try {
				resultSet.close();
				resultSet = null;
			} catch (Throwable e) {
			}
			closeStatement(stm);
		}
	}

	public static void closeStatement(Statement statement) {
		if (statement == null)
			return;
		try {
			statement.close();
			statement = null;
		} catch (Throwable e) {
		}
	}

	public static ArrayList<String> listJndi() throws Exception {
		HashMap<String, String> buffer = Var.getServerVar();
		Set<Entry<String, String>> es = buffer.entrySet();
		ArrayList<String> list = new ArrayList<String>(buffer.size());
		String k;

		for (Entry<String, String> e : es) {
			k = e.getKey();
			if (k.toLowerCase().indexOf("jndi") != -1)
				list.add("Var.server." + k);
		}
		return list;
	}

	public static void startTrans(Connection connection, String isolation)
			throws Exception {
		if (!connection.getAutoCommit())
			connection.commit();
		connection.setAutoCommit(false);
		if (!StringUtil.isEmpty(isolation)) {
			if (isolation.equals("readUncommitted"))
				connection
						.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			else if (isolation.equals("readCommitted"))
				connection
						.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			else if (isolation.equals("repeatableRead"))
				connection
						.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			else if (isolation.equals("serializable"))
				connection
						.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		}
	}

	public static boolean isBlobField(int type) {
		switch (type) {
		case -2:
		case -3:
		case -4:
		case 2004:
			return true;
		}
		return false;
	}

	public static boolean isTextField(int type) {
		switch (type) {
		case -1:
		case -16:
		case 2005:
		case 2011:
			return true;
		}
		return false;
	}

	public static boolean isLargeField(int type) {
		switch (type) {
		case -1:
		case -2:
		case -3:
		case -4:
		case -16:
		case 2004:
		case 2005:
		case 2011:
			return true;
		}
		return false;
	}

	public static void loadFirstRow(HttpServletRequest request, String queryName)
			throws Exception {
		ResultSet resultSet = (ResultSet) request.getAttribute(queryName);
		if (!resultSet.next())
			return;
		ResultSetMetaData meta = resultSet.getMetaData();
		int i, j = meta.getColumnCount(), type;
		String name;

		for (i = 1; i <= j; i++) {
			type = meta.getColumnType(i);
			name = queryName + "." + meta.getColumnLabel(i);
			if (isBlobField(type))
				request.setAttribute(name, resultSet.getBinaryStream(i));
			else
				request
						.setAttribute(name, DbUtil
								.getObject(resultSet, i, type));
		}
	}

	public static String getTypeCategory(int type) {
		switch (type) {
		case Types.BIGINT:
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
			return "int";
		case Types.DECIMAL:
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.NUMERIC:
		case Types.REAL:
			return "float";
		case Types.TIMESTAMP:
		case Types.DATE:
		case Types.TIME:
			return "date";
		case Types.BOOLEAN:
		case Types.BIT:
			return "bool";
		default:
			return "string";
		}
	}

	public static String getFields(ResultSetMetaData meta, int colCount,
			boolean dateAsString, JSONObject keyMap) throws Exception {
		int i, type;
		String name, category, fmt;
		StringBuilder buf = new StringBuilder();

		buf.append('[');
		for (i = 0; i < colCount; i++) {
			if (i > 0)
				buf.append(',');
			buf.append("{name:");
			name = meta.getColumnLabel(i + 1);
			if (StringUtil.isEmpty(name))
				name = "FIELD" + Integer.toString(i + 1);
			buf.append(StringUtil.quote(name));
			buf.append(",type:\"");
			if (keyMap != null && keyMap.has(name))
				type = Types.VARCHAR;
			else
				type = meta.getColumnType(i + 1);
			if (dateAsString
					&& (type == Types.TIMESTAMP || type == Types.DATE || type == Types.TIME))
				type = Types.VARCHAR;
			category = getTypeCategory(type);
			buf.append(category);
			switch (type) {
			case Types.TIMESTAMP:
			case Types.DATE:
				fmt = "Y-m-d H:i:s.u";
				break;
			case Types.TIME:
				fmt = "H:i:s";
				break;
			default:
				fmt = null;
			}
			if (fmt != null) {
				buf.append("\",dateFormat:\"");
				buf.append(fmt);
			}
			buf.append("\"}");
		}
		buf.append(']');
		return buf.toString();
	}

	public static String getColumns(ResultSetMetaData meta, int colCount,
			String editorType, boolean rowNumber, boolean dateAsString,
			JSONObject keyMap) throws Exception {
		int i, k, len, type, editor;
		boolean isText, isBlob, isMap;
		String field, name, validator, renderer;
		StringBuilder buf = new StringBuilder();

		if (editorType.equals("editable"))
			editor = 1;
		else if (editorType.equals("readOnly"))
			editor = 2;
		else
			editor = 0;
		buf.append('[');
		if (rowNumber)
			buf
					.append("{width:45,sortable:false,hideable:false,type:\"rowNumber\",renderer:\"Wb.nr\",align:\"right\"}");
		for (i = 0; i < colCount; i++) {
			if (rowNumber || i > 0)
				buf.append(',');
			field = meta.getColumnLabel(i + 1);
			if (StringUtil.isEmpty(field))
				field = "FIELD" + Integer.toString(i + 1);
			name = StringUtil.quote(field);
			buf.append("{dataIndex:");
			buf.append(name);
			buf.append(",header:");
			buf.append(name);
			isMap = keyMap != null && keyMap.has(field);
			if (isMap)
				type = Types.VARCHAR;
			else
				type = meta.getColumnType(i + 1);
			isText = isTextField(type);
			isBlob = isBlobField(type);
			renderer = "Wb.htmlRender";
			validator = "";
			if (isMap)
				len = 20;
			else if (isText)
				len = 25;
			else if (isBlob)
				len = 6;
			else
				len = meta.getColumnDisplaySize(i + 1);
			switch (type) {
			case Types.TIMESTAMP:
			case Types.DATE:
				if (editor == 1)
					validator = "Wb.tsValidator";
				if (!dateAsString)
					renderer = "Wb.dateRender";
				len = 18;
				break;
			case Types.TIME:
				if (editor == 1)
					validator = "Wb.tmValidator";
				if (!dateAsString)
					renderer = "Wb.timeRender";
				len = 10;
				break;
			case Types.BIGINT:
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.NUMERIC:
			case Types.REAL:
				buf.append(",align:\"right\"");
				if (editor == 1)
					validator = "Wb.numValidator";
				break;
			}
			if (editor == 1 && isBlob)
				buf.append(",renderer:\"Wb.downRender('" + field + "')\"");
			else {
				buf.append(",renderer:");
				buf.append(StringUtil.quote(renderer));
			}
			buf.append(",sortable:true,width:");
			k = name.length() - 1;
			if (len < k)
				len = k;
			if (len < 5)
				len = 5;
			if (len > 25)
				len = 25;
			buf.append(len * 9);
			if (editor > 0 && !isBlob) {
				buf.append(",editor:{xtype:\"");
				if (isText)
					buf.append("textarea\",height:100");
				else
					buf.append("textfield\"");
				if (meta.isNullable(i + 1) == ResultSetMetaData.columnNoNulls)
					buf.append(",allowBlank:false");
				if (isMap || isBlob || editor == 2 || meta.isReadOnly(i + 1))
					buf
							.append(",readOnly:true,fieldStyle:\"background-image:none;background-color:#C0C0C0\"");
				if (!validator.isEmpty()) {
					buf.append(",validator:");
					buf.append(validator);
				}
				buf.append('}');
			}
			buf.append('}');
		}
		buf.append(']');
		return buf.toString();
	}

	public static Object query(HttpServletRequest request, String sql,
			boolean beginTrans, String jndi, String type, boolean loadData)
			throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("id", SysUtil.getId());
		jo.put("sql", sql);
		jo.put("jndi", jndi);
		jo.put("type", type);
		jo.put("loadData", loadData);
		jo.put("transaction", beginTrans ? "start" : "");
		Query query = new Query();
		query.request = request;
		query.xwlObject = jo;
		query.create();
		return query.result;
	}

	public static ResultSet query(HttpServletRequest request, String sql)
			throws Exception {
		return (ResultSet) query(request, sql, false, null, "query", false);
	}

	public static ResultSet query(HttpServletRequest request, String sql,
			String jndi) throws Exception {
		return (ResultSet) query(request, sql, false, jndi, "query", false);
	}

	public static ResultSet query(HttpServletRequest request, String sql,
			String jndi, boolean loadData) throws Exception {
		return (ResultSet) query(request, sql, false, jndi, "query", loadData);
	}

	public static int update(HttpServletRequest request, String sql)
			throws Exception {
		return (Integer) query(request, sql, false, null, "update", false);
	}

	public static int update(HttpServletRequest request, String sql, String jndi)
			throws Exception {
		return (Integer) query(request, sql, false, jndi, "update", false);
	}

	public static int update(HttpServletRequest request, String sql,
			String jndi, boolean beginTrans) throws Exception {
		return (Integer) query(request, sql, beginTrans, jndi, "update", false);
	}

	public static Object execute(HttpServletRequest request, String sql)
			throws Exception {
		return query(request, sql, false, null, "execute", false);
	}

	public static Object execute(HttpServletRequest request, String sql,
			String jndi) throws Exception {
		return query(request, sql, false, jndi, "execute", false);
	}

	public static Object execute(HttpServletRequest request, String sql,
			String jndi, boolean beginTrans) throws Exception {
		return query(request, sql, beginTrans, jndi, "execute", false);
	}

	public static void outputBlob(ResultSet resultSet,
			HttpServletRequest request, HttpServletResponse response,
			boolean download) throws Exception {
		InputStream inputStream = null;
		OutputStream outputStream;
		int rowCount = resultSet.getMetaData().getColumnCount();
		String name = "blob.bin", size = null;

		try {
			response.reset();
			if (resultSet.next()) {
				switch (rowCount) {
				case 1:
					inputStream = resultSet.getBinaryStream(1);
					break;
				case 2:
					name = resultSet.getString(2);
					inputStream = resultSet.getBinaryStream(1);
					break;
				case 3:
					name = resultSet.getString(2);
					size = resultSet.getString(3);
					inputStream = resultSet.getBinaryStream(1);
					break;
				}
			} else
				throw new Exception(Str.format(request, "objectNotExist"));
			outputStream = response.getOutputStream();
			if (download)
				response
						.setHeader("content-type", "application/force-download");
			else
				response.setHeader("content-type", "application/octet-stream");
			response.setHeader("content-disposition", "attachment;"
					+ WebUtil.encodeFilename(request, name));
			if (size != null)
				response.setHeader("content-length", size);
			if (inputStream != null)
				SysUtil.isToOs(inputStream, outputStream);
			response.flushBuffer();
		} finally {
			SysUtil.closeInputStream(inputStream);
		}
	}

	public static void outputImage(ResultSet resultSet,
			HttpServletRequest request, HttpServletResponse response,
			String format) throws Exception {
		InputStream inputStream = null;
		OutputStream outputStream;
		int rowCount = resultSet.getMetaData().getColumnCount();
		String name = "file", size = null;

		try {
			response.reset();
			if (StringUtil.isEqual(format, "image"))
				format = null;
			if (resultSet.next()) {
				switch (rowCount) {
				case 1:
					inputStream = resultSet.getBinaryStream(1);
					break;
				case 2:
					name = resultSet.getString(2);
					if (format == null)
						format = FileUtil.extractFileExt(name);
					inputStream = resultSet.getBinaryStream(1);
					break;
				case 3:
					name = resultSet.getString(2);
					size = resultSet.getString(3);
					if (format == null)
						format = FileUtil.extractFileExt(name);
					inputStream = resultSet.getBinaryStream(1);
					break;
				}
			}
			outputStream = response.getOutputStream();
			if (inputStream == null) {
				inputStream = new FileInputStream(new File(Main.path,
						"webbuilder/images/null.gif"));
				format = "gif";
			}
			if (format != null)
				response.setContentType("image/" + format);
			response.setHeader("content-disposition", "attachment;"
					+ WebUtil.encodeFilename(request, name));
			if (size != null)
				response.setHeader("content-length", size);
			if (inputStream != null)
				SysUtil.isToOs(inputStream, outputStream);
			response.flushBuffer();
		} finally {
			SysUtil.closeInputStream(inputStream);
		}
	}

	public static void exportData(Writer writer, ResultSet rs) throws Exception {
		ResultSetMetaData meta = rs.getMetaData();
		int i, j = meta.getColumnCount(), ct = 0, types[] = new int[j];

		writer.write("[[");
		for (i = 0; i < j; i++) {
			types[i] = meta.getColumnType(i + 1);
			if (i > 0)
				writer.write(',');
			writer.write(meta.getColumnName(i + 1));
		}
		writer.write(']');
		while (rs.next()) {
			writer.write(",[");
			for (i = 0; i < j; i++) {
				if (i > 0)
					writer.write(',');
				writer.write(StringUtil.encode(DbUtil.getObject(rs, i + 1,
						types[i], false)));
			}
			writer.write(']');
			ct++;
		}
		writer.write(']');
		writer.flush();
	}

	public static String getArray(ResultSet rs) throws Exception {
		StringBuilder buf = new StringBuilder();
		ResultSetMetaData meta = rs.getMetaData();
		int i, j = meta.getColumnCount(), ct = 0, types[] = new int[j], limit = Var
				.getInt("webbuilder.control.limitRecords");
		boolean isFirst = true;

		for (i = 0; i < j; i++)
			types[i] = meta.getColumnType(i + 1);
		buf.append('[');
		while (rs.next()) {
			if (ct > limit)
				break;
			if (isFirst)
				isFirst = false;
			else
				buf.append(',');
			if (j > 1) {
				buf.append('[');
				for (i = 0; i < j; i++) {
					if (i > 0)
						buf.append(',');
					buf.append(StringUtil.encode(DbUtil.getObject(rs, i + 1,
							types[i], true)));
				}
				buf.append(']');
			} else
				buf.append(StringUtil.encode(DbUtil.getObject(rs, 1, types[0],
						true)));
			ct++;
		}
		buf.append(']');
		return buf.toString();
	}

	public static String getCharStream(ResultSet rs, int index)
			throws Exception {
		return (String) getObject(rs, index, -1);
	}

	public static void setCharStream(PreparedStatement st, int index,
			String value) throws Exception {
		setObject(st, index, -1, value);
	}

	public static Object getObject(CallableStatement st, int index, int type)
			throws Exception {
		switch (type) {
		case Types.CHAR:
		case Types.NCHAR:
		case Types.VARCHAR:
		case Types.NVARCHAR:
			return st.getString(index);
		case Types.INTEGER:
			return st.getInt(index);
		case Types.TINYINT:
			return st.getByte(index);
		case Types.SMALLINT:
			return st.getShort(index);
		case Types.BIGINT:
			return st.getLong(index);
		case Types.REAL:
		case Types.FLOAT:
			return st.getFloat(index);
		case Types.DOUBLE:
			return st.getDouble(index);
		case Types.DECIMAL:
		case Types.NUMERIC:
			return st.getBigDecimal(index);
		case Types.TIMESTAMP:
		case Types.DATE:
			return st.getTimestamp(index);
		case Types.TIME:
			return st.getTime(index);
		case Types.BOOLEAN:
		case Types.BIT:
			return st.getBoolean(index);
		case Types.LONGVARCHAR:
		case Types.LONGNVARCHAR:
		case Types.CLOB:
		case Types.NCLOB:
			Reader rd = st.getCharacterStream(index);
			if (rd == null)
				return null;
			else
				return SysUtil.readString(rd);
		default:
			return st.getObject(index);
		}
	}

	public static Object getObject(ResultSet rs, int index, int type)
			throws Exception {
		return getObject(rs, index, type, true);
	}

	public static Object getObject(ResultSet rs, int index, int type,
			boolean ignoreBlob) throws Exception {
		Object obj;
		switch (type) {
		case Types.CHAR:
		case Types.NCHAR:
		case Types.VARCHAR:
		case Types.NVARCHAR:
			obj = rs.getString(index);
			break;
		case Types.INTEGER:
			obj = rs.getInt(index);
			break;
		case Types.TINYINT:
			obj = rs.getByte(index);
			break;
		case Types.SMALLINT:
			obj = rs.getShort(index);
			break;
		case Types.BIGINT:
			obj = rs.getLong(index);
			break;
		case Types.REAL:
		case Types.FLOAT:
			obj = rs.getFloat(index);
			break;
		case Types.DOUBLE:
			obj = rs.getDouble(index);
			break;
		case Types.DECIMAL:
		case Types.NUMERIC:
			obj = rs.getBigDecimal(index);
			break;
		case Types.TIMESTAMP:
		case Types.DATE:
			obj = rs.getTimestamp(index);
			break;
		case Types.TIME:
			obj = rs.getTime(index);
			break;
		case Types.BOOLEAN:
		case Types.BIT:
			obj = rs.getBoolean(index);
			break;
		case Types.LONGVARCHAR:
		case Types.LONGNVARCHAR:
		case Types.CLOB:
		case Types.NCLOB:
			Reader rd = rs.getCharacterStream(index);
			if (rd == null)
				obj = null;
			else
				obj = SysUtil.readString(rd);
			break;
		case Types.BLOB:
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
			InputStream is = rs.getBinaryStream(index);
			if (ignoreBlob) {
				if (is == null)
					return "(blob)";
				else {
					is.close();
					return "(BLOB)";
				}
			} else
				return is;
		default:
			obj = rs.getObject(index);
		}
		if (rs.wasNull())
			return null;
		else
			return obj;
	}

	public static void setObject(PreparedStatement st, int index, int type,
			Object obj) throws Exception {
		if (obj != null && !(obj instanceof String)) {
			if (obj instanceof InputStream)
				st.setBinaryStream(index, (InputStream) obj,
						((InputStream) obj).available());
			else if (obj instanceof java.util.Date)
				st.setTimestamp(index, new Timestamp(((java.util.Date) obj)
						.getTime()));
			else {
				if (type != 12)
					st.setObject(index, obj, type);
				else
					st.setObject(index, obj);
			}
			return;
		}
		String value;

		if (obj == null)
			value = null;
		else
			value = (String) obj;
		if (StringUtil.isEmpty(value))
			st.setNull(index, type);
		else {
			switch (type) {
			case Types.CHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
				st.setString(index, value);
				break;
			case Types.INTEGER:
				st.setInt(index, Integer.parseInt(value));
				break;
			case Types.TINYINT:
				st.setByte(index, Byte.parseByte(value));
				break;
			case Types.SMALLINT:
				st.setShort(index, Short.parseShort(value));
				break;
			case Types.BIGINT:
				st.setLong(index, Long.parseLong(value));
				break;
			case Types.REAL:
			case Types.FLOAT:
				st.setFloat(index, Float.parseFloat(value));
				break;
			case Types.DOUBLE:
				st.setDouble(index, Double.parseDouble(value));
				break;
			case Types.DECIMAL:
			case Types.NUMERIC:
				st.setBigDecimal(index, new BigDecimal(value));
				break;
			case Types.TIMESTAMP:
			case Types.DATE:
				st.setTimestamp(index, Timestamp.valueOf(DateUtil
						.fixTimestamp(value)));
				break;
			case Types.TIME:
				st.setTime(index, Time.valueOf(DateUtil.fixTime(value)));
				break;
			case Types.BOOLEAN:
			case Types.BIT:
				st.setBoolean(index, StringUtil.getBool(value));
				break;
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.CLOB:
			case Types.NCLOB:
				st.setCharacterStream(index, new StringReader(value), value
						.length());
				break;
			case Types.BLOB:
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				InputStream is = new ByteArrayInputStream(StringUtil
						.decodeBase64(value));
				st.setBinaryStream(index, is, is.available());
				break;
			default:
				st.setObject(index, value, type);
			}
		}
	}
}
