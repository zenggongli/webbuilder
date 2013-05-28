package com.webbuilder.tool;

import java.math.RoundingMode;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Var;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;

public class PrintObject {
	public static final String printPath = "webbuilder.service.download.print.";

	public static String preview(JSONArray data, JSONArray meta, String title,
			String dateFormat, String timeFormat, String previewText,
			String numText, String numWidth, String thousandSeparator,
			String decimalSeparator) throws Exception {
		ArrayList<JSONObject> children = new ArrayList<JSONObject>();
		int flexColWidth = Var
				.getInt(ExcelObject.excelPath + "flexColumnWidth");
		boolean isRate[];
		double doubleVal;
		String fontName, names[], aligns[], selFormat, value;
		String header = createHeaders(children, meta, numText, numWidth,
				flexColWidth);
		StringBuilder buf = new StringBuilder();
		DecimalFormat df;
		DecimalFormatSymbols dfs;
		JSONObject jo;
		int i, j, x, y = children.size(), types[], width = 0;
		Format formats[], defaultDateFormat, defaultTimeFormat, defaultDTFormat;
		boolean isTime;
		Date dateValue;

		defaultDateFormat = new SimpleDateFormat(toJavaDateFormat(dateFormat));
		defaultTimeFormat = new SimpleDateFormat(toJavaDateFormat(timeFormat));
		defaultDTFormat = new SimpleDateFormat(toJavaDateFormat(dateFormat
				+ " " + timeFormat));
		types = new int[y];
		names = new String[y];
		aligns = new String[y];
		formats = new Format[y];
		isRate = new boolean[y];
		for (x = 0; x < y; x++) {
			jo = children.get(x);
			names[x] = jo.optString("dataIndex");
			types[x] = ExcelObject.getFieldType(jo);
			aligns[x] = JsonUtil.optString(jo, "align");
			selFormat = JsonUtil.optString(jo, "ptFormat");
			width += jo.isNull("width") ? flexColWidth : jo.optInt("width");
			if (selFormat.isEmpty()) {
				selFormat = JsonUtil.optString(jo, "jsFormat");
				if (!selFormat.isEmpty()) {
					if (types[x] == 5)
						formats[x] = new SimpleDateFormat(
								toJavaDateFormat(selFormat));
					else {
						formats[x] = new DecimalFormat(
								toJavaNumFormat(selFormat));
						df = ((DecimalFormat) formats[x]);
						df.setRoundingMode(RoundingMode.HALF_UP);
						dfs = new DecimalFormatSymbols();
						dfs.setDecimalSeparator(decimalSeparator.charAt(0));
						dfs.setGroupingSeparator(thousandSeparator.charAt(0));
						df.setDecimalFormatSymbols(dfs);
					}
				}
			}
			isRate[x] = selFormat.endsWith("%");
		}
		if (numWidth == null)
			width += 3 * (y - 1);
		else
			width += Integer.parseInt(numWidth) + 3 * y;
		buf
				.append("<!DOCTYPE html><html><head><meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"><title>");
		if (StringUtil.isEmpty(title))
			buf.append(previewText);
		else
			buf.append(title);
		buf
				.append("</title><style type=\"text/css\">table{border:1px solid #000000;table-layout:fixed;border-collapse:collapse");
		fontName = Var.get(printPath + "fontFamily");
		if (!StringUtil.isEmpty(fontName)) {
			buf.append(";font-family:");
			buf.append(fontName);
		}
		buf
				.append("}td{border:1px solid #000000;word-wrap:break-word;word-break:break-all;font-size:");
		buf.append(Var.get(printPath + "fontSize"));
		buf.append("px}</style></head><body>");
		if (!StringUtil.isEmpty(title)) {
			buf.append("<p style=\"text-align:center;font-size:");
			buf.append(Var.get(printPath + "titleFontSize"));
			buf.append("px");
			if (Var.getBool(printPath + "titleFontBold"))
				buf.append(";font-weight:bold");
			fontName = Var.get(printPath + "titleFontFamily");
			if (!StringUtil.isEmpty(fontName)) {
				buf.append(";font-family:");
				buf.append(fontName);
			}
			buf.append(";width:");
			buf.append(width);
			buf.append("px\">");
			buf.append(toHTML(title));
			buf.append("</p>");
		}
		buf.append("<table width=\"");
		buf.append(width);
		buf.append("\"");
		buf.append(" border=\"1px\" cellspacing=\"0px\" cellpadding=\"3px\">");
		buf.append(header);
		j = data.length();
		for (i = 0; i < j; i++) {
			buf.append("<tr>");
			if (numText != null) {
				buf.append("<td align=\"right\"");
				buf.append(">");
				buf.append(Integer.toString(i + 1));
				buf.append("</td>");
			}
			jo = data.getJSONObject(i);
			for (x = 0; x < y; x++) {
				value = JsonUtil.optString(jo, names[x]);
				if (!value.isEmpty()) {
					switch (types[x]) {
					case 2:
					case 3:
						if (formats[x] != null) {
							doubleVal = Double.parseDouble(value);
							if (isRate[x])
								doubleVal = doubleVal / 100;
							value = formats[x].format(doubleVal);
						} else if (value.endsWith(".0"))
							value = value.substring(0, value.length() - 2);
						break;
					case 5:
						isTime = value.indexOf(' ') == -1;
						if (isTime)
							dateValue = Time.valueOf(value);
						else
							dateValue = Timestamp.valueOf(value);
						if (formats[x] != null)
							value = formats[x].format(dateValue);
						else if (isTime)
							value = defaultTimeFormat.format(dateValue);
						else if (value.endsWith("00:00:00.0"))
							value = defaultDateFormat.format(dateValue);
						else
							value = defaultDTFormat.format(dateValue);
						if (value.endsWith("_X")) {
							value = value.substring(0, value.length() - 2)
									+ (DateUtil.hourOfDay(dateValue) < 12 ? "AM"
											: "PM");
						}
						break;
					}
				}
				buf.append("<td align=\"");
				buf.append(aligns[x]);
				buf.append("\">");
				buf.append(toHTML(value));
				buf.append("</td>");
			}
			buf.append("</tr>");
		}
		buf.append("</table></body></html>");
		return buf.toString();
	}

	private static String toJavaNumFormat(String format) {
		int i = format.indexOf('.');

		if (i == -1)
			i = format.length() - 1;
		else
			i--;
		return StringUtil.replace(format.substring(0, i), "0", "#")
				+ format.substring(i);
	}

	private static String toJavaDateFormat(String format) {
		String[][] map = { { "y", "yy" }, { "Y", "yyyy" }, { "m", "MM" },
				{ "n", "M" }, { "d", "dd" }, { "j", "d" }, { "H", "HH" },
				{ "h", "hh" }, { "G", "H" }, { "g", "h" }, { "i", "mm" },
				{ "s", "ss" }, { "u", "SSS" }, { "A", "'_X'" } };

		for (String[] s : map) {
			format = StringUtil.replace(format, s[0], s[1]);
		}
		return format;
	}

	private static String createHeaders(ArrayList<JSONObject> children,
			JSONArray columns, String numText, String numWidth, int flexColWidth)
			throws Exception {
		HashMap<JSONObject, JSONObject> relations = JsonUtil.getRelations(
				columns, children, "columns");
		int x, z, depth, maxDepth = 0;
		String bgColor;
		JSONObject jo, parent;
		StringBuilder buf = new StringBuilder();

		for (JSONObject o : children) {
			depth = 1;
			parent = o;
			while ((parent = relations.get(parent)) != null) {
				parent.put("colspan", parent.optInt("colspan") + 1);
				depth++;
			}
			o.put("depth", depth);
			maxDepth = Math.max(maxDepth, depth);
		}
		for (x = 0; x < maxDepth; x++) {
			buf.append("<tr style=\"");
			bgColor = Var.get(printPath + "headerFontBgColor");
			if (!StringUtil.isEmpty(bgColor)) {
				buf.append(";background-color:#");
				buf.append(bgColor);
			}
			if (Var.getBool(printPath + "headerFontBold"))
				buf.append(";font-weight:bold");
			buf.append("\">");
			if (x == 0 && numText != null) {
				buf.append("<td align=\"right\"");
				if (maxDepth > 1) {
					buf.append(" rowspan=\"");
					buf.append(maxDepth);
					buf.append("\"");
				}
				buf.append(" width=\"");
				buf.append(numWidth);
				buf.append("\">");
				buf.append(toHTML(numText));
				buf.append("</td>");
			}
			for (JSONObject item : children) {
				depth = item.optInt("depth");
				jo = getAt(relations, item, x);
				if (jo != null) {
					if (x == depth - 1)
						z = maxDepth - (depth - 1);
					else
						z = 1;
					buf.append("<td align=\"");
					buf.append(x == depth - 1 ? jo.optString("align")
							: "center");
					buf.append("\"");
					if (z > 1) {
						buf.append(" rowspan=\"");
						buf.append(z);
						buf.append("\"");
					}
					z = jo.optInt("colspan");
					if (z > 1) {
						buf.append(" colspan=\"");
						buf.append(z);
						buf.append("\"");
					}
					if (!jo.has("columns")) {
						buf.append(" width=\"");
						buf.append(jo.isNull("width") ? flexColWidth : jo
								.optInt("width"));
						buf.append("\"");
					}
					buf.append(">");
					buf.append(toHTML(JsonUtil.optString(jo, "text")));
					buf.append("</td>");
				}
			}
			buf.append("</tr>");
		}
		return buf.toString();
	}

	private static String toHTML(String text) {
		return StringUtil.toHTML(text, true, true);
	}

	private static JSONObject getAt(HashMap<JSONObject, JSONObject> relations,
			JSONObject jo, int depth) throws Exception {
		int i = 1, z = jo.getInt("depth");
		JSONObject parent = jo;

		do {
			if (z - i == depth)
				break;
			i++;
		} while ((parent = relations.get(parent)) != null);
		if (parent != null) {
			if (parent.optBoolean("xwlProcessed"))
				return null;
			parent.put("xwlProcessed", true);
		}
		return parent;
	}
}
