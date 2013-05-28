package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Attribute extends BackControl {
	public void create() throws Exception {
		boolean ow = gb("overwrite", true);
		String name = gs("name");

		if (StringUtil.isEmpty(name))
			name = gs("id");
		if (ow || ga(name) == null)
			request.setAttribute(name, gs("value"));
	}
}
