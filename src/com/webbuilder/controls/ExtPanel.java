package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class ExtPanel extends ExtControl {
	private String key[] = { "bodyStyle", "border", "bgColor", "bgImage",
			"transparent" };

	protected String getTagProperties() throws Exception {
		String border = gs("border"), bgColor = gs("bgColor"), bgImage = gs("bgImage");

		setStyles(gs("bodyStyle"));
		if (gb("transparent", false)) {
			bgColor = "transparent";
			if (StringUtil.isEmpty(border))
				border = "false";
		}
		if (StringUtil.isEmpty(bgImage)) {
			if (!StringUtil.isEmpty(bgColor))
				bgImage = "none";
		} else
			bgImage = "url(" + bgImage + ")";
		addStyle("background-color", bgColor);
		addStyle("background-image", bgImage);
		addText("bodyStyle", getSBuffer());
		addExpress("border", border);
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
