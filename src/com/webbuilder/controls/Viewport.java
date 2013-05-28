package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Viewport extends ExtControl {
	public void create() throws Exception {
		String id = gs("id");
		headerScript(StringUtil.concat("Wd.", id, "=new " + getMeta("xwlType")
				+ "({"));
		footerScript("});");
		headerScript(getJson(null));
		if (hasChild) {
			headerScriptNL(",items:[");
			footerScript("]");
		}
	}
}
