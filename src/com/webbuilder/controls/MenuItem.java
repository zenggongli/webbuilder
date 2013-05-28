package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class MenuItem extends ExtControl {
	private String key[] = { "type", "text", "toolType", "select", "popup",
			"module", "hidden", "ignoreParentClicks", "minWidth", "plain",
			"showSeparator" };
	private String text;
	private boolean hasText = true;

	public void create() throws Exception {
		String xtype = "", type = gs("type"), menuType = "menu";

		text = gs("text");
		if (StringUtil.isEqual(text, "-")) {
			if (StringUtil.isEqual(parentControl.optString("xwlMeta"),
					"menuItem"))
				xtype = "menuseparator";
			else
				xtype = "tbseparator";
			hasText = false;
		} else if (StringUtil.isEqual(text, " ")) {
			xtype = "tbspacer";
			hasText = false;
		} else if (StringUtil.isEqual(text, "->")) {
			xtype = "tbfill";
			hasText = false;
		} else if (StringUtil.isEqual(type, "textItem"))
			xtype = "tbtext";
		else if (StringUtil.isEqual(type, "split"))
			xtype = "splitbutton";
		else if (StringUtil.isEqual(type, "colorPicker"))
			menuType = "colormenu";
		else if (StringUtil.isEqual(type, "datePicker"))
			menuType = "datemenu";
		if (!StringUtil.isEmpty(xtype))
			xtype = StringUtil.concat("xtype:\"", xtype, "\"");
		headerScript(StringUtil.concat(getComma(), "{", getJson(xtype)));
		footerScript("}");
		if (hasChild || !StringUtil.isEqual(menuType, "menu")) {
			String p = gs("popup"), z = gs("select");
			if (!StringUtil.isEmpty(p))
				p = StringUtil
						.concat(",listeners:{show:function(menu,options){\n",
								p, "\n}}");
			else
				p = "";
			if (!StringUtil.isEmpty(z))
				z = StringUtil.concat(
						",handler:function(menu,value,options){\n", z, "\n}");
			else
				z = "";
			headerScriptNL(StringUtil.concat(",menu:{xtype:\"", menuType, "\"",
					getMenuExpress(), p, z));
			if (hasChild) {
				headerScriptNL(",items:[");
				footerScript("]}");
			} else
				footerScript("}");
		}
	}

	protected String getTagProperties() throws Exception {
		String module = gs("module"), hidden = gs("hidden");

		if (!module.isEmpty()
				&& !WebUtil.checkRight(request, WebUtil.getUrl(module, true)))
			hidden = "true";
		addExpress("hidden", hidden);
		if (!hasText)
			return getPBuffer();
		addText("text", text);
		addText("type", gs("toolType"));
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}

	private String getMenuExpress() throws Exception {
		int i, j = key.length;
		String s;

		resetPBuffer();
		for (i = 7; i < j; i++) {
			s = key[i];
			addExpress(s, gs(s));
		}
		s = getPBuffer();
		if (StringUtil.isEmpty(s))
			return "";
		else
			return "," + s;
	}
}
