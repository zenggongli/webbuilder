package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Toolbar extends ExtControl {
	private String key[] = { "titleToolbar" };

	public void create() throws Exception {
		if (gb(key[0], false)) {
			String id = gs("id");
			headerScript(StringUtil.concat("Wd.", id, "=["));
			footerScript("];");
		} else {
			setRender = false;
			createScript();
		}
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
