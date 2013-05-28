package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Ajax extends ExtControl {
	private String key[] = { "id", "params" };

	public void create() throws Exception {
		listenersPrefix = false;
		String id = gs("id"), exp = getJson(null);
		StringBuilder buf = new StringBuilder();

		buf.append("Wd.");
		buf.append(id);
		buf.append("={request:function(p){Wb.request(");
		buf.append(id);
		buf.append(",p)}");
		if (!StringUtil.isEmpty(exp)) {
			buf.append(',');
			buf.append(exp);
		}
		buf.append("};");
		headerScript(buf.toString());
	}

	protected String getTagProperties() throws Exception {
		String p = gs("params");

		if (StringUtil.isEmpty(p))
			p = "{}";
		addExpress("params", p);
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
