package com.webbuilder.controls;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Str;
import com.webbuilder.common.Var;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;

public class Query extends BackControl {
	public Object result;
	private Object[][] typeMap;
	private String id, jndi, sql, an, type, transaction, isolation;
	private boolean loadData, uniqueUpdate;
	private ArrayList<String> paramList = new ArrayList<String>();
	private ArrayList<String> outParamList = new ArrayList<String>();
	private int paramSize;
	private int batchBufferSize;
	private Connection connection;
	private PreparedStatement statement;
	private static String[] sqlTypes = { "VARCHAR=12", "INTEGER=4",
			"TIMESTAMP=93", "DOUBLE=8", "TEXT=-1", "BLOB=2004",
			"LONGVARCHAR=-1", "NUMERIC=2", "DECIMAL=3", "SMALLINT=5",
			"BIGINT=-5", "TINYINT=-6", "FLOAT=6", "REAL=7", "DOUBLE=8",
			"CHAR=1", "BIT=-7", "DATE=91", "TIME=92", "BINARY=-2",
			"VARBINARY=-3", "LONGVARBINARY=-4", "NULL=0", "OTHER=1111",
			"JAVA_OBJECT=2000", "DISTINCT=2001", "STRUCT=2002", "ARRAY=2003",
			"CLOB=2005", "REF=2006", "DATALINK=70", "BOOLEAN=16", "ROWID=-8",
			"NCHAR=-15", "NVARCHAR=-9", "LONGNVARCHAR=-16", "NCLOB=2011",
			"SQLXML=2009" };

	public void create() throws Exception {
		getProperties();
		if (StringUtil.isEmpty(sql) || gb("disabled", false))
			return;
		boolean isCall, hasRs = false;

		if (jndi.startsWith("Var."))
			jndi = Var.get(jndi.substring(4));
		connection = DbUtil.getConnection(request, jndi);
		if (uniqueUpdate && transaction.isEmpty() && connection.getAutoCommit())
			transaction = "start";
		if (StringUtil.isEqual(transaction, "start"))
			DbUtil.startTrans(connection, isolation);
		try {
			replaceSqlParameters();
			isCall = sql.substring(0, 1).equals("{");
			if (isCall) {
				statement = connection.prepareCall(sql);
				type = null;
			} else
				statement = connection.prepareStatement(sql);
			if (paramSize > 0)
				regParameters();
			if (!StringUtil.isEmpty(an))
				executeBatch(an);
			else if (StringUtil.isEqual(type, "query")) {
				result = statement.executeQuery();
				request.setAttribute(id, result);
				hasRs = true;
				if (loadData)
					DbUtil.loadFirstRow(request, id);
			} else if (StringUtil.isEqual(type, "update")) {
				result = statement.executeUpdate();
				if (uniqueUpdate && ((Integer) result) != 1)
					throw new Exception(Str.format(request, "recordNotUnique"));
				request.setAttribute(id, result);
			} else {
				if (statement.execute()) {
					result = statement.getResultSet();
					request.setAttribute(id, result);
					hasRs = true;
					if (loadData)
						DbUtil.loadFirstRow(request, id);
				} else {
					result = statement.getUpdateCount();
					if (uniqueUpdate && ((Integer) result) != 1)
						throw new Exception(Str.format(request,
								"recordNotUnique"));
					request.setAttribute(id, result);
				}
				if (isCall && paramSize > 0)
					getOutParameter();
			}
			if (transaction.equals("commit")) {
				connection.commit();
				connection.setAutoCommit(true);
			}
		} finally {
			if (!hasRs)
				DbUtil.closeStatement(statement);
		}
	}

	private void executeBatch(String an) throws Exception {
		Object obj = ga(an), types[];
		JSONArray ja;
		JSONObject jo;
		String val;
		int i, j, k, l;
		boolean commitAll = false;

		if (obj instanceof JSONArray)
			ja = (JSONArray) obj;
		else {
			if (obj == null)
				return;
			val = obj.toString();
			if (val.isEmpty())
				return;
			ja = new JSONArray(val);
		}
		j = ja.length();
		if (j == 0)
			return;
		l = typeMap.length;
		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			for (k = 0; k < l; k++) {
				types = typeMap[k];
				if (jo.has((String) types[1]))
					DbUtil.setObject(statement, k + 1, (Integer) types[0],
							JsonUtil.opt(jo, (String) types[1]));
			}
			statement.addBatch();
			if ((i + 1) % batchBufferSize == 0) {
				statement.executeBatch();
				if (i == j - 1)
					commitAll = true;
			}
		}
		if (!commitAll)
			statement.executeBatch();
	}

	private void getProperties() throws Exception {
		id = gs("id");
		jndi = gs("jndi");
		sql = gs("sql");
		String switcher = gs("sqlSwitcher");
		if (sql.isEmpty() && !switcher.isEmpty())
			sql = gp(switcher);
		sql = sql.trim();
		type = gs("type");
		an = gs("arrayName");
		transaction = gs("transaction");
		uniqueUpdate = gb("uniqueUpdate", false);
		isolation = gs("isolation");
		loadData = gb("loadData", false);
		batchBufferSize = gi("batchBufferSize", Integer.MAX_VALUE);
	}

	private int getSqlType(String type) {
		int i, j = sqlTypes.length;

		for (i = 0; i < j; i++)
			if (StringUtil.isSame(StringUtil.getNamePart(sqlTypes[i]), type))
				return Integer.parseInt(StringUtil.getValuePart(sqlTypes[i]));
		if (StringUtil.isNumeric(type, false))
			return Integer.parseInt(type);
		else
			return 12;
	}

	private boolean isSqlType(String type) {
		int i, j = sqlTypes.length;

		if (StringUtil.isNumeric(type, false))
			return true;
		for (i = 0; i < j; i++)
			if (StringUtil.isSame(StringUtil.getNamePart(sqlTypes[i]), type))
				return true;
		return false;
	}

	private void regParameters() throws Exception {
		String paraName, param, typeText;
		Object params[];
		int i, dotPos, type, subType;
		boolean hasSub;
		CallableStatement callStatement;

		if (statement instanceof CallableStatement)
			callStatement = (CallableStatement) statement;
		else
			callStatement = null;
		typeMap = new Object[paramSize][2];
		for (i = 0; i < paramSize; i++) {
			param = paramList.get(i);
			if (callStatement != null
					&& StringUtil.substring(param, 0, 1).equals("@")) {
				param = param.substring(1);
				dotPos = param.indexOf('.');
				if (dotPos == -1) {
					typeText = "varchar";
					paraName = param;
				} else {
					typeText = param.substring(0, dotPos);
					paraName = param.substring(dotPos + 1);
				}
				hasSub = typeText.indexOf('=') != -1;
				if (hasSub) {
					type = getSqlType(StringUtil.getNamePart(typeText));
					subType = Integer.parseInt(StringUtil
							.getValuePart(typeText));
					callStatement.registerOutParameter(i + 1, type, subType);
				} else {
					type = getSqlType(typeText);
					callStatement.registerOutParameter(i + 1, type);
				}
				outParamList.add(StringUtil.concat(Integer.toString(type), "=",
						paraName));
			} else {
				params = parseParam(param);
				typeMap[i] = params;
				DbUtil.setObject(statement, i + 1, (Integer) params[0],
						ga((String) params[1]));
				outParamList.add("");
			}
		}
	}

	private Object[] parseParam(String param) {
		String type, name;
		Object result[] = new Object[2];
		int dotPos = param.indexOf('.');
		if (dotPos == -1) {
			type = "VARCHAR";
			name = param;
		} else {
			type = param.substring(0, dotPos);
			if (isSqlType(type))
				name = param.substring(dotPos + 1);
			else {
				type = "VARCHAR";
				name = param;
			}
		}
		result[0] = getSqlType(type);
		result[1] = name;
		return result;
	}

	private void getOutParameter() throws Exception {
		CallableStatement st = (CallableStatement) statement;
		String para;
		int i;

		for (i = 0; i < paramSize; i++) {
			para = outParamList.get(i);
			if (!StringUtil.isEmpty(para))
				request.setAttribute(StringUtil.getValuePart(para), DbUtil
						.getObject(st, i + 1, Integer.parseInt(StringUtil
								.getNamePart(para))));
		}
	}

	private void replaceSqlParameters() {
		String param;
		int startPos, endPos;

		while ((startPos = sql.indexOf("{?")) > -1
				&& (endPos = sql.indexOf("?}")) > -1) {
			param = sql.substring(startPos + 2, endPos);
			sql = StringUtil.replaceFirst(sql, "{?" + param + "?}", "?");
			paramList.add(param);
		}
		paramSize = paramList.size();
	}
}
