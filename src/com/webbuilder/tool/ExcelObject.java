package com.webbuilder.tool;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Var;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;

public class ExcelObject {
	public static final String excelPath = "webbuilder.service.download.excel.";

	public static String getColName(int colIndex) {
		String ch;

		if (colIndex < 26)
			ch = String.valueOf((char) ((colIndex) + 65));
		else
			ch = String.valueOf((char) ((colIndex) / 26 + 65 - 1))
					+ String.valueOf((char) ((colIndex) % 26 + 65));
		return ch;
	}

	public static byte[] getExcelBytes(JSONArray data, JSONArray columnsDefine,
			String groupName, String title, String sheetName,
			String dateFormat, String timeFormat, String totalText,
			String thousandSeparator, String decimalSeparator) throws Exception {
		JSONObject jo;
		HSSFWorkbook book = new HSSFWorkbook();
		HSSFSheet sheet = book.createSheet("Sheet1");
		HSSFRow row;
		HSSFCell cell;
		Short bgColor;
		HashMap<String, HSSFCellStyle> cachedStyle = new HashMap<String, HSSFCellStyle>();
		HSSFCellStyle blankStyle, styles[], style, summaryStyles[];
		boolean nullStyle, isRate[];
		int i, j = data.length(), k = 0, x, y, startRow, types[], curRow, dateTypes[];
		short rowHeight = (short) Var.getInt(excelPath + "rowHeight");
		double doubleVal;
		String selFormat, value, names[], aligns[];
		ArrayList<JSONObject> children = new ArrayList<JSONObject>();

		if (!StringUtil.isEmpty(sheetName))
			book.setSheetName(0, sheetName);
		value = Var.get(excelPath + "headerFontBgColor");
		if (StringUtil.isEmpty(value))
			bgColor = null;
		else
			bgColor = Short.parseShort(value);
		dateFormat = toExcelDateFormat(dateFormat);
		timeFormat = toExcelDateFormat(timeFormat);
		if (StringUtil.isEmpty(title) || !Var.getBool(excelPath + "showTitle")) {
			startRow = 0;
			row = null;
		} else {
			startRow = 1;
			row = sheet.createRow(0);
			row.setHeight((short) Var.getInt(excelPath + "titleHeight"));
		}
		startRow = createHeaders(children, sheet, book, bgColor, columnsDefine,
				startRow);
		y = children.size();
		if (Var.getBool(excelPath + "freezeHeader"))
			sheet.createFreezePane(0, startRow);
		if (row != null) {
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, y - 1));
			cell = row.createCell(0);
			cell.setCellStyle(createTitleStyle(book));
			cell.setCellValue(title);
		}
		styles = new HSSFCellStyle[y];
		summaryStyles = new HSSFCellStyle[y];
		names = new String[y];
		types = new int[y];
		isRate = new boolean[y];
		aligns = new String[y];
		dateTypes = new int[y];
		for (x = 0; x < y; x++) {
			jo = children.get(x);
			styles[x] = createRowStyle(book, jo, null);
			summaryStyles[x] = createRowStyle(book, jo, bgColor);
			names[x] = jo.optString("dataIndex");
			types[x] = getFieldType(jo);
			selFormat = JsonUtil.optString(jo, "format");
			if (selFormat.isEmpty())
				selFormat = JsonUtil.optString(jo, "jsFormat");
			isRate[x] = selFormat.endsWith("%");
			aligns[x] = JsonUtil.optString(jo, "align");
			dateTypes[x] = 0;
		}
		blankStyle = getCellStyle(cachedStyle, book, "", "", null);
		j = data.length();
		for (i = 0; i < j; i++) {
			jo = data.getJSONObject(i);
			curRow = i + startRow + k;
			row = sheet.createRow(curRow);
			row.setHeight(rowHeight);
			for (x = 0; x < y; x++) {
				cell = row.createCell(x);
				nullStyle = styles[x] == null;
				if (!nullStyle)
					cell.setCellStyle(styles[x]);
				value = JsonUtil.optString(jo, names[x]);
				if (value.isEmpty()) {
					if (nullStyle)
						cell.setCellStyle(blankStyle);
				} else {
					switch (types[x]) {
					case 2:
					case 3:
						doubleVal = Double.parseDouble(value);
						if (isRate[x])
							doubleVal = doubleVal / 100;
						cell.setCellValue(doubleVal);
						break;
					case 4:
						cell.setCellValue(StringUtil.getBool(value));
						break;
					case 5:
						if (value.indexOf(' ') != -1) {
							if (nullStyle) {
								if (value.endsWith("00:00:00.0")) {
									style = getCellStyle(cachedStyle, book,
											dateFormat, aligns[x], null);
									cell.setCellStyle(style);
									if (dateTypes[x] == 0)
										dateTypes[x] = 2;
									else if (dateTypes[x] == 1)
										dateTypes[x] = 3;
								} else {
									style = getCellStyle(cachedStyle, book,
											dateFormat + " " + timeFormat,
											aligns[x], null);
									cell.setCellStyle(style);
									if (dateTypes[x] < 3)
										dateTypes[x] = 3;
								}
							}
							cell.setCellValue(Timestamp.valueOf(value));
						} else if (value.indexOf('-') != -1) {
							if (nullStyle) {
								style = getCellStyle(cachedStyle, book,
										dateFormat, aligns[x], null);
								cell.setCellStyle(style);
								if (dateTypes[x] == 0)
									dateTypes[x] = 2;
								else if (dateTypes[x] == 1)
									dateTypes[x] = 3;
							}
							cell.setCellValue(java.sql.Date.valueOf(value));
						} else {
							if (nullStyle) {
								style = getCellStyle(cachedStyle, book,
										timeFormat, aligns[x], null);
								cell.setCellStyle(style);
								if (dateTypes[x] == 0)
									dateTypes[x] = 1;
							}
							cell.setCellValue(java.sql.Time.valueOf(value));
						}
						break;
					default:
						cell.setCellValue(value);
					}
				}
			}
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		book.write(os);
		return os.toByteArray();
	}

	private static HSSFCellStyle getCellStyle(
			HashMap<String, HSSFCellStyle> cache, HSSFWorkbook book,
			String format, String align, Short bgColor) throws Exception {
		String key = StringUtil.concat(format, ".", align, ".",
				bgColor == null ? "x" : Short.toString(bgColor));
		HSSFCellStyle style = cache.get(key);
		if (style == null) {
			style = createRowStyle(book, new JSONObject(StringUtil.concat(
					"{format:", StringUtil.quote(format), ",align:\"", align,
					"\"}")), bgColor);
			cache.put(key, style);
		}
		return style;
	}

	public static int getFieldType(JSONObject jo) {
		String types[] = { "auto", "string", "int", "float", "boolean", "date" };
		return StringUtil.indexOf(types, JsonUtil.optString(jo, "type"));
	}

	private static int createHeaders(ArrayList<JSONObject> children,
			HSSFSheet sheet, HSSFWorkbook book, Short bgColor,
			JSONArray columns, int startRow) throws Exception {
		HashMap<JSONObject, JSONObject> relations = JsonUtil.getRelations(
				columns, children, "columns");
		int i, depth, maxDepth = 1, x, y, w, z, height, offset, index, flexColWidth;
		HSSFRow row;
		HSSFCell cell;
		JSONObject parent, jco;
		HashMap<String, HSSFCell> map = new HashMap<String, HSSFCell>();

		for (JSONObject jo : children) {
			depth = 1;
			parent = jo;
			while ((parent = relations.get(parent)) != null) {
				parent.put("size", parent.optInt("size") + 1);
				depth++;
			}
			jo.put("depth", depth);
			maxDepth = Math.max(maxDepth, depth);
		}
		y = children.size();
		height = Var.getInt(excelPath + "headerHeight");
		flexColWidth = Var.getInt(excelPath + "flexColumnWidth");
		for (i = 0; i < maxDepth; i++) {
			row = sheet.createRow(i + startRow);
			row.setHeight((short) height);
			for (x = 0; x < y; x++) {
				cell = row.createCell(x);
				jco = children.get(x);
				depth = jco.getInt("depth");
				cell.setCellStyle(createHeaderStyle(book, bgColor, jco,
						i < depth - 1));
				map.put(Integer.toString(x) + "," + Integer.toString(i), cell);
			}
		}
		x = 0;
		for (JSONObject jo : children) {
			y = maxDepth - 1;
			depth = jo.getInt("depth");
			offset = maxDepth - depth;
			if (offset > 0)
				sheet.addMergedRegion(new CellRangeAddress(y - offset
						+ startRow, y + startRow, x, x));
			cell = map.get(Integer.toString(x) + ","
					+ Integer.toString(y - offset));
			cell.setCellValue(JsonUtil.optString(jo, "text"));
			if (jo.isNull("width"))
				w = flexColWidth;
			else
				w = jo.getInt("width");
			sheet.setColumnWidth(x, (int) (w * 36.55d));
			if (jo.optBoolean("hidden"))
				sheet.setColumnHidden(x, true);
			parent = jo;
			index = 0;
			z = maxDepth - offset - 1;
			while ((parent = relations.get(parent)) != null) {
				if (!parent.optBoolean("xwlProcessed")) {
					index++;
					sheet.addMergedRegion(new CellRangeAddress(z - index
							+ startRow, z - index + startRow, x, x
							+ parent.getInt("size") - 1));
					cell = map.get(Integer.toString(x) + ","
							+ Integer.toString(z - index));
					cell.setCellValue(JsonUtil.optString(parent, "text"));
					parent.put("xwlProcessed", true);
				}
			}
			x++;
		}
		return maxDepth + startRow;
	}

	private static HSSFCellStyle createTitleStyle(HSSFWorkbook book)
			throws Exception {
		HSSFCellStyle style = book.createCellStyle();
		HSSFFont font = book.createFont();
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		style.setWrapText(true);
		String fn = Var.get(excelPath + "titleFontName");
		if (!StringUtil.isEmpty(fn))
			font.setFontName(fn);
		font.setBoldweight((short) Var.getInt(excelPath + "titleFontBold"));
		font.setFontHeight((short) Var.getInt(excelPath + "titleFontHeight"));
		style.setFont(font);
		return style;
	}

	private static HSSFCellStyle createHeaderStyle(HSSFWorkbook book,
			Short bgColor, JSONObject jo, boolean center) throws Exception {
		HSSFCellStyle style = book.createCellStyle();
		HSSFFont font = book.createFont();
		String align;
		if (center)
			align = "center";
		else {
			align = JsonUtil.optString(jo, "headerAlign");
			if (align.isEmpty())
				align = JsonUtil.optString(jo, "align");
		}
		style.setAlignment(getAlign(align));
		style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		if (bgColor != null) {
			style.setFillForegroundColor(bgColor);
			style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		}
		style.setWrapText(true);
		style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		style.setBorderRight(HSSFCellStyle.BORDER_THIN);
		String v = Var.get(excelPath + "headerFontName");
		if (!StringUtil.isEmpty(v))
			font.setFontName(v);
		font.setBoldweight((short) Var.getInt(excelPath + "headerFontBold"));
		font.setFontHeight((short) Var.getInt(excelPath + "headerFontHeight"));
		v = Var.get(excelPath + "headerFontColor");
		if (!StringUtil.isEmpty(v))
			font.setColor(Short.parseShort(v));
		style.setFont(font);
		return style;
	}

	private static HSSFCellStyle createRowStyle(HSSFWorkbook book,
			JSONObject jo, Short bgColor) throws Exception {
		String format = JsonUtil.optString(jo, "format");
		String type = JsonUtil.optString(jo, "type");
		String jsFormat = JsonUtil.optString(jo, "jsFormat");

		if (format.isEmpty() && !jsFormat.isEmpty()) {
			if (type.equals("date"))
				format = toExcelDateFormat(jsFormat);
			else
				format = toExcelNumFormat(jsFormat);
		}
		if (format.isEmpty()) {
			if (type.equals("string"))
				format = "@";
			else if (type.equals("date"))
				return null;
		}
		HSSFCellStyle style = book.createCellStyle();
		HSSFFont font = book.createFont();
		style.setAlignment(getAlign(jo.optString("align")));
		style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		if (jo.optBoolean("autoWrap"))
			style.setWrapText(true);
		String v = Var.get(excelPath + "rowFontName");
		if (!StringUtil.isEmpty(v))
			font.setFontName(v);
		font.setBoldweight((short) Var.getInt(excelPath + "rowFontBold"));
		font.setFontHeight((short) Var.getInt(excelPath + "rowFontHeight"));
		if (!format.isEmpty())
			style.setDataFormat(book.createDataFormat().getFormat(format));
		style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		style.setBorderRight(HSSFCellStyle.BORDER_THIN);
		if (bgColor != null) {
			style.setFillForegroundColor(bgColor);
			style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		}
		style.setFont(font);
		return style;
	}

	private static String toExcelNumFormat(String format) {
		int i = format.indexOf('.');

		if (i == -1)
			i = format.length() - 1;
		else
			i--;
		return StringUtil.replace(format.substring(0, i), "0", "#")
				+ format.substring(i);
	}

	private static String toExcelDateFormat(String format) {
		if (StringUtil.isEmpty(format))
			return "";
		String[][] map = { { "y", "yy" }, { "Y", "yyyy" }, { "m", "mm" },
				{ "n", "m" }, { "d", "dd" }, { "j", "d" }, { "H", "hh" },
				{ "h", "hh" }, { "G", "h" }, { "g", "h" }, { "i", "mm" },
				{ "s", "ss" }, { "u", "000" }, { "/", "\"/\"" },
				{ "A", "AM/PM" } };

		for (String[] s : map) {
			format = StringUtil.replace(format, s[0], s[1]);
		}
		return format;
	}

	private static short getAlign(String align) {
		if (align.equals("right"))
			return HSSFCellStyle.ALIGN_RIGHT;
		else if (align.equals("center"))
			return HSSFCellStyle.ALIGN_CENTER;
		else
			return HSSFCellStyle.ALIGN_LEFT;
	}

	public static Object getCellValue(HSSFCell cell) {
		switch (cell.getCellType()) {
		case HSSFCell.CELL_TYPE_FORMULA:
		case HSSFCell.CELL_TYPE_NUMERIC:
			return cell.getNumericCellValue();
		case HSSFCell.CELL_TYPE_STRING:
			return cell.getStringCellValue();
		case HSSFCell.CELL_TYPE_BOOLEAN:
			return cell.getBooleanCellValue();
		default:
			return null;
		}
	}
}