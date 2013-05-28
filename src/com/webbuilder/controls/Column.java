package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Column extends ExtControl {
	private String key[] = { "renderer", "align", "width", "sortable",
			"hideable", "style" };
	private String type;

	public void create() throws Exception {
		String xtype;

		type = gs("type");
		if (type.equals("treeColumn"))
			xtype = "xtype:\"treecolumn\"";
		else
			xtype = null;
		headerScript(StringUtil.concat(getComma(), "{", getJson(xtype)));
		footerScript("}");
		if (hasChild) {
			headerScriptNL(",columns:[");
			footerScript("]");
		}
	}

	protected String getTagProperties() throws Exception {
		String render = gs("renderer"), align = gs("align"), format = gs("format");
		String width = gs("width"), sortable = gs("sortable"), hideable = gs("hideable"), style = gs("style"), headerAlign = gs("headerAlign");
		boolean isRowNumber = type.equals("rowNumber");

		if (isRowNumber) {
			if (width.isEmpty())
				width = "45";
			if (sortable.isEmpty())
				sortable = "false";
			if (align.isEmpty())
				align = "right";
			if (hideable.isEmpty())
				hideable = "false";
		}
		if (format.isEmpty())
			format = "0";
		else
			format = StringUtil.quote(format);
		if (StringUtil.isEmpty(render)) {
			if (!hasChild) {
				if (isRowNumber)
					addExpress("renderer", "Wb.nr");
				else
					addExpress("renderer", StringUtil.concat(
							"function(a,b){return Wb.rd(a,b,", gb("autoWrap",
									false) ? "1" : "0", ",", format, ")}"));
			}
		} else
			addExpress(
					"renderer",
					StringUtil
							.concat(
									"function(value,metaData,record,rowIndex,colIndex,store,view){\n",
									render, "\n}"));
		addExpress("sortable", sortable);
		addExpress("hideable", hideable);
		addExpress("width", width);
		addText("align", align);
		if (!headerAlign.isEmpty()) {
			if (!style.isEmpty() && !style.endsWith(";"))
				style += ";";
			style += "text-align:" + headerAlign + ";";
		}
		addText("style", style);
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
