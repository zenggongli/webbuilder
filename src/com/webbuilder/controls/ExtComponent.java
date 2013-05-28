package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class ExtComponent extends ExtControl {
	private String key[] = { "id" };

	public void create() throws Exception {
		String type = getMeta("xwlType");
		headerScript(StringUtil.concat("Wd.", gs("id"), "=new ", type, "({"));
		footerScript("});");
		headerScript(getJson(null));
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}