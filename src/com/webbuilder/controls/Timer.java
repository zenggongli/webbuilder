package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Timer extends FrontControl {
	public void create() throws Exception {
		String timeout = gs("timeout"), interval = gs("interval");
		String onTimeout = gs("onTimeout"), onInterval = gs("onInterval");
		String id = gs("id");

		if (!StringUtil.isEmpty(timeout) && !StringUtil.isEmpty(onTimeout))
			headerScript(StringUtil.concat("Wd.", id,
					"=setTimeout(function(){\n" + onTimeout + "\n},", timeout,
					");"));
		if (!StringUtil.isEmpty(onInterval) && !StringUtil.isEmpty(onInterval))
			headerScript(StringUtil.concat("Wd.", id,
					"=setInterval(function(){\n" + onInterval + "\n},",
					interval, ");"));
	}
}
