package com.webbuilder.controls;

import com.webbuilder.common.ScriptBuffer;
import com.webbuilder.utils.StringUtil;

public class ServerScript extends BackControl {
	public void create() throws Exception {
		String s = gs("script");
		if (!StringUtil.isEmpty(s))
			ScriptBuffer.run(
					StringUtil.concat(xwlId, ".", gs("id"), ".script"), s,
					request, response);
	}
}
