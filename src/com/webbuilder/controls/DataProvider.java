package com.webbuilder.controls;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Str;
import com.webbuilder.common.SysMap;
import com.webbuilder.tool.PageInfo;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class DataProvider extends FrontControl {
	private static String treePrototypes = "[{name:'parentId',type:'auto',defaultValue:null},{name:'index',type:'int',defaultValue:null,persist:false},{name:'depth',type:'int',defaultValue:0,persist:false},{name:'expanded',type:'bool',defaultValue:false,persist:false},{name:'expandable',type:'bool',defaultValue:true,persist:false},{name:'checked',type:'auto',defaultValue:null,persist:false},{name:'leaf',type:'bool',defaultValue:false},{name:'cls',type:'string',defaultValue:null,persist:false},{name:'iconCls',type:'string',defaultValue:null,persist:false},{name:'icon',type:'string',defaultValue:null,persist:false},{name:'root',type:'boolean',defaultValue:false,persist:false},{name:'isLast',type:'boolean',defaultValue:false,persist:false},{name:'isFirst',type:'boolean',defaultValue:false,persist:false},{name:'allowDrop',type:'boolean',defaultValue:true,persist:false},{name:'allowDrag',type:'boolean',defaultValue:true,persist:false},{name:'loaded',type:'boolean',defaultValue:false,persist:false},{name:'loading',type:'boolean',defaultValue:false,persist:false},{name:'href',type:'string',defaultValue:null,persist:false},{name:'hrefTarget',type:'string',defaultValue:null,persist:false},{name:'qtip',type:'string',defaultValue:null,persist:false},{name:'qtitle',type:'string',defaultValue:null,persist:false},{name:'children',type:'auto',defaultValue:null,persist:false}]";
	private JSONObject keyMap;
	private boolean hasKey;

	public void create() throws Exception {
		ResultSet rs;
		Object obj;
		String type = gs("type"), sql, switcher, jndi, valStr, mapFields = gs("keyMap");
		boolean isTree = type.equals("tree"), isArray = type.isEmpty()
				|| type.equals("jsonArray") || isTree;
		PageInfo pageInfo;

		if (mapFields.isEmpty())
			keyMap = null;
		else
			keyMap = new JSONObject(mapFields);
		hasKey = keyMap != null;
		if (isArray) {
			pageInfo = WebUtil.getPage(request);
			String ls = gs("limitRecords");
			if (!ls.isEmpty()) {
				pageInfo.limit = Integer.parseInt(ls);
				if (pageInfo.limit == -1)
					pageInfo.limit = Integer.MAX_VALUE;
			}
		} else
			pageInfo = null;
		setOrderSql();
		sql = gs("sql");
		switcher = gs("sqlSwitcher");
		jndi = gs("jndi");
		if (sql.isEmpty() && !switcher.isEmpty())
			sql = gp(switcher);
		obj = run(sql, jndi, gs("resultSet"));
		if (obj instanceof ResultSet)
			rs = (ResultSet) obj;
		else {
			if (obj == null)
				valStr = "\"\"";
			else
				valStr = StringUtil.quote(obj.toString());
			WebUtil
					.response(
							response,
							StringUtil
									.concat(
											"{total:1,metaData:{fields:[{name:\"result\",type:\"string\"}]},columns:[{dataIndex:\"result\",header:",
											StringUtil.quote(Str.format(
													request, "result")),
											",width:150}],returnResult:",
											valStr, ",rows:[{result:", valStr,
											"}]}"));
			return;
		}
		if (isArray) {
			Integer totalCount = null;
			String totalSql, totalString = gs("totalCount");
			if (totalString.isEmpty()) {
				totalSql = gs("totalSql");
				switcher = gs("totalSqlSwitcher");
				if (totalSql.isEmpty() && !switcher.isEmpty())
					totalSql = gp(switcher);
				obj = run(totalSql, jndi, gs("totalResultSet"));
				if (obj instanceof ResultSet) {
					ResultSet totalRs = (ResultSet) obj;
					if (totalRs.next())
						totalCount = totalRs.getInt(1);
				} else if (obj != null)
					totalCount = Integer.parseInt(obj.toString());
			} else
				totalCount = Integer.parseInt(totalString);
			outputJsonArray(rs, totalCount, pageInfo, isTree);
		} else if (type.equals("jsonObject"))
			outputJsonObject(rs);
		else if (type.equals("download"))
			DbUtil.outputBlob(rs, request, response, true);
		else if (type.equals("stream"))
			DbUtil.outputBlob(rs, request, response, false);
		else
			DbUtil.outputImage(rs, request, response, type);
	}

	private Object run(String sql, String jndi, String name) throws Exception {
		Object obj, attr;

		if (sql.isEmpty())
			obj = null;
		else
			obj = DbUtil.execute(request, sql, jndi);
		if (name.isEmpty())
			return obj;
		else {
			attr = ga(name);
			if (attr == null)
				return obj;
			else
				return attr;
		}
	}

	private void outputJsonArray(ResultSet rs, Integer total,
			PageInfo pageInfo, boolean isTree) throws Exception {
		int i, j, cp;
		boolean first = true, needTotal, addComma, paged = gb("autoPage", true);
		Object obj;
		String names[], labels[], val, fields, xVal, tagInfo = gs("tag");
		int types[];
		StringBuilder buf = new StringBuilder();
		ResultSetMetaData meta = rs.getMetaData();
		boolean dateAsString = gb("dateAsString", false);

		j = meta.getColumnCount();
		names = new String[j];
		labels = new String[j];
		types = new int[j];
		for (i = 0; i < j; i++) {
			names[i] = meta.getColumnLabel(i + 1);
			if (StringUtil.isEmpty(names[i]))
				names[i] = "FIELD" + Integer.toString(i + 1);
			labels[i] = StringUtil.quote(names[i]);
			types[i] = meta.getColumnType(i + 1);
		}
		if (total != null) {
			buf.append("{total:");
			buf.append(Integer.toString(total));
			needTotal = false;
		} else
			needTotal = true;
		if (!tagInfo.isEmpty()) {
			buf.append(",");
			buf.append(tagInfo);
		}
		buf.append(",metaData:{fields:");
		fields = DbUtil.getFields(meta, j, dateAsString, keyMap);
		if (isTree)
			buf.append(mergeFields(fields));
		else
			buf.append(fields);
		buf.append('}');
		if (gb("createColumns", false)) {
			buf.append(",columns:");
			buf.append(DbUtil.getColumns(meta, j, gs("editorType"), gb(
					"rowNumber", false), dateAsString, keyMap));
		}
		if (isTree)
			buf.append(",children:[");
		else
			buf.append(",rows:[");
		while (rs.next()) {
			cp = WebUtil.checkPage(pageInfo, paged, needTotal);
			if (cp == 1)
				break;
			else if (cp == 2)
				continue;
			if (first)
				first = false;
			else
				buf.append(',');
			buf.append('{');
			addComma = false;
			for (i = 0; i < j; i++) {
				obj = DbUtil.getObject(rs, i + 1, types[i]);
				if (obj == null)
					continue;
				if (hasKey && keyMap.has(names[i]))
					obj = SysMap
							.get(keyMap.getString(names[i]), obj.toString());
				if (addComma)
					buf.append(',');
				else
					addComma = true;
				buf.append(labels[i]);
				buf.append(':');
				if (dateAsString && obj instanceof Timestamp) {
					buf.append('"');
					val = obj.toString();
					if (val.endsWith("00:00:00.0"))
						val = val.substring(0, 10);
					else if (val.endsWith(".0"))
						val = val.substring(0, 19);
					buf.append(val);
					buf.append('"');
				} else {
					if (isTree && obj != null) {
						xVal = obj.toString();
						if (xVal.equals("__[]"))
							xVal = "[]";
						else if (xVal.equals("__true"))
							xVal = "true";
						else if (xVal.equals("__false"))
							xVal = "false";
						else
							xVal = StringUtil.encode(obj);
						buf.append(xVal);
					} else
						buf.append(StringUtil.encode(obj));
				}
			}
			buf.append('}');
		}
		buf.append("]}");
		if (needTotal)
			WebUtil.setTotal(buf, pageInfo);
		WebUtil.setCb(buf, request.getParameter("callback"));
		WebUtil.response(response, buf);
	}

	private String mergeFields(String fields) throws Exception {
		JSONArray pt = new JSONArray(treePrototypes), ja = new JSONArray(fields);
		int i, j = pt.length(), k, l = ja.length();
		HashSet<String> hs = new HashSet<String>(j);
		JSONObject jo;

		for (i = 0; i < j; i++)
			hs.add(pt.getJSONObject(i).getString("name"));
		for (k = 0; k < l; k++) {
			jo = ja.getJSONObject(k);
			if (!hs.contains(jo.getString("name")))
				pt.put(jo);
		}
		return pt.toString();
	}

	private void outputJsonObject(ResultSet rs) throws Exception {
		JSONObject jo = new JSONObject();

		if (rs.next()) {
			int i, j, type;
			ResultSetMetaData meta = rs.getMetaData();
			String key;
			Object value;

			j = meta.getColumnCount();
			for (i = 0; i < j; i++) {
				type = meta.getColumnType(i + 1);
				key = meta.getColumnLabel(i + 1);
				if (StringUtil.isEmpty(key))
					key = "FIELD" + Integer.toString(i + 1);
				value = DbUtil.getObject(rs, i + 1, type);
				if (hasKey && value != null && keyMap.has(key))
					value = SysMap.get(keyMap.getString(key), value.toString());
				jo.put(key, value);
			}
		}
		WebUtil.response(response, jo);
	}

	private void setOrderSql() throws Exception {
		String sort = gp("sort");

		if (!sort.isEmpty()) {
			JSONArray ja = new JSONArray(sort);
			int i, j = ja.length();
			if (j > 0) {
				JSONObject jo;
				StringBuilder exp = new StringBuilder();
				String property, defaultPrefix, prefix, orderFields = gs("orderFields");
				JSONObject orderJo;

				if (orderFields.isEmpty()) {
					orderJo = null;
					defaultPrefix = "";
				} else {
					orderJo = new JSONObject(orderFields);
					defaultPrefix = JsonUtil.optString(orderJo, "default");
				}
				for (i = 0; i < j; i++) {
					jo = ja.getJSONObject(i);
					if (i > 0)
						exp.append(',');
					property = jo.getString("property");
					if (orderJo != null) {
						if (orderJo.has(property)) {
							prefix = orderJo.optString(property);
							if (!prefix.isEmpty()) {
								exp.append(prefix);
								exp.append(".");
							}
						} else if (!defaultPrefix.isEmpty()) {
							exp.append(defaultPrefix);
							exp.append(".");
						}
					}
					exp.append(property);
					if (StringUtil.isSame(jo.optString("direction"), "desc"))
						exp.append(" desc");
				}
				request.setAttribute("sql.orderBy", " order by " + exp);
				request.setAttribute("sql.orderFields", "," + exp);
			}
		}
	}
}
