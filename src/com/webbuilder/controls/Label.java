package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Label extends ExtControl {
	private String key[] = { "align", "style", "y", "height", "color" };

	protected String getTagProperties() throws Exception {
		String y = gs("y"), color = gs("color");

		if (!StringUtil.isEmpty(y)) {
			y = y + "+4";
			addExpress("y", y);
		}
		setStyles(gs("style"));
		addStyle("text-align", gs("align"));
		addStyle("color", color);
		addText("style", getSBuffer());
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
