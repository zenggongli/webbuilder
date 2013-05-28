package com.webbuilder.controls;

import com.webbuilder.utils.SysUtil;

public class Method extends BackControl {
	public void create() throws Exception {
		SysUtil.executeMethod(gs("methodName"), request, response);
	}
}
