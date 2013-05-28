package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class ExtField extends ExtControl {
	private String key[] = { "color", "bgColor", "bgImage", "fieldStyle",
			"height", "autoCreate", "inputType" };
	private boolean isFile;
	private String inputType;

	public void create() throws Exception {
		boolean autoCreate = gb("autoCreate", true);
		inputType = gs("inputType");
		isFile = StringUtil.isEqual(inputType, "file");
		String id = gs("id");

		if (hasParent || !autoCreate) {
			String xtype;

			if (isFile)
				xtype = "filefield";
			else
				xtype = getMeta("xwlXtype");
			if (StringUtil.isEmpty(xtype))
				xtype = "";
			else
				xtype = StringUtil.concat("xtype:\"", xtype, "\"");
			if (hasParent)
				headerScript(StringUtil.concat(getComma(), "{", getJson(xtype)));
			else
				headerScript(StringUtil.concat("Wd.", id, "={", getJson(xtype)));
			footerScript("}");
			if (hasChild) {
				headerScriptNL(",items:[");
				footerScript("]");
			}
		} else {
			String type;
			if (isFile)
				type = "Ext.form.field.File";
			else
				type = getMeta("xwlType");
			headerScript(StringUtil.concat("Wd.", id, "=new ", type, "({"));
			footerScript("});");
			headerScript(getJson("renderTo:Ext.getBody()"));
			if (hasChild) {
				headerScriptNL(",items:[");
				footerScript("]");
			}
		}
	}

	protected String getTagProperties() throws Exception {
		String color = gs("color"), bgColor = gs("bgColor"), bgImage = gs("bgImage");

		setStyles(gs("fieldStyle"));
		if (StringUtil.isEmpty(bgImage)) {
			if (!StringUtil.isEmpty(bgColor))
				bgImage = "none";
		} else
			bgImage = StringUtil.concat("url(", bgImage, ")");
		addStyle("color", color);
		addStyle("background-color", bgColor);
		addStyle("background-image", bgImage);
		if (isFile) {
			addText("name", gs("id"));
			addExpress("buttonConfig", "{iconCls:\"explorer_icon\",text:\"\"}");
		} else
			addText("inputType", inputType);
		addText("fieldStyle", getSBuffer());
		if (getMeta("xwlXtype").equals("textarea"))
			addExpress("height", gs("height"));
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
