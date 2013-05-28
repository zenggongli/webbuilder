package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Button extends ExtControl {
	private String key[] = { "color", "fontSize", "height", "text", "module",
			"hidden", "afterrender" };

	protected String getTagProperties() throws Exception {
		String color = gs("color"), text = gs("text"), spanStyle, scale = gs("scale");
		String module = gs("module"), hidden = gs("hidden");

		addStyle("color", color);
		addStyle("font-size", gs("fontSize"));
		spanStyle = getSBuffer();
		if (text.startsWith("@"))
			text = text.substring(1);
		else
			text = StringUtil.quote(text);
		if (!StringUtil.isEmpty(spanStyle))
			text = StringUtil.concat("\"<span style='", spanStyle, "'>\"+",
					text, "+\"</span>\"");
		addExpress("text", text);
		if (!module.isEmpty()
				&& !WebUtil.checkRight(request, WebUtil.getUrl(module, true)))
			hidden = "true";
		addExpress("hidden", hidden);
		if (StringUtil.isEmpty(scale))
			addExpress("height", gs("height"));
		return getPBuffer();
	}

	protected String getTagEvents() throws Exception {
		String afterrender = gs("afterrender");

		if (!gs("bgColor").isEmpty() || !gs("bgImage").isEmpty()) {
			if (afterrender.isEmpty())
				afterrender = "Wb.setButton(button);";
			else
				afterrender = "Wb.setButton(button);\n" + afterrender;
		}
		if (!afterrender.isEmpty())
			addEvent("afterrender", StringUtil.concat(
					"function(button,options){\n", afterrender, "\n}"));
		return getEBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
