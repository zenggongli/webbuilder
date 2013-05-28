package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Form extends ExtPanel {
	private String key[] = { "beforerequest", "failure", "success", "params" };

	protected String getTagProperties() throws Exception {
		String br = gs("beforerequest"), failure = gs("failure"), success = gs("success"), params = gs("params");

		super.getTagProperties();
		if (params.isEmpty())
			params = "{}";
		addExpress("params", params);
		if (!StringUtil.isEmpty(br))
			addExpress("beforerequest", StringUtil.concat("function(form){\n",
					br, "\n}"));
		if (!StringUtil.isEmpty(failure))
			addExpress("failure", StringUtil.concat(
					"function(form,action,value){\n", failure, "\n}"));
		if (!StringUtil.isEmpty(success))
			addExpress("success", StringUtil.concat(
					"function(form,action,value){\n", success, "\n}"));
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return StringUtil.merge(super.getReservedKeys(), key);
	}
}
