package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class JA extends ExtControl {
	public void create() throws Exception {
		headerScript(StringUtil.concat("Wd.", gs("id"), "=["));
		footerScript("];");
	}
}
