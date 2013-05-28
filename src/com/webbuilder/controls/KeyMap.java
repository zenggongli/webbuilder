package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class KeyMap extends ExtControl {
	private String key[] = { "target", "id" };

	public void create() throws Exception {
		String target = gs("target"), json = getJson(null);
		if (target.isEmpty())
			target = "document";
		else
			target = "\"" + target + "\"";
		if (!json.isEmpty())
			target = target + ",";
		finalScript(StringUtil.concat("Wd.", gs("id"),
				"=new Ext.util.KeyMap({target:", target, json, "});"));
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
