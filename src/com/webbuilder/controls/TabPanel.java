package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class TabPanel extends ExtPanel {
	private String key[] = { "plugins", "tabMenu" };

	protected String getTagProperties() throws Exception {
		String plugins = gs("plugins"), exp;

		super.getTagProperties();
		if (gb("tabMenu", true)) {
			exp = "{ptype:\"tabscrollermenu\"}";
			if (StringUtil.isEmpty(plugins))
				plugins = exp;
			else {
				if (plugins.endsWith("]"))
					plugins = plugins.substring(0, plugins.length() - 1) + ","
							+ exp + "]";
				else
					plugins += "," + exp;
			}
		}
		if (!plugins.startsWith("[") && plugins.indexOf(',') != -1)
			plugins = "[" + plugins + "]";
		addExpress("plugins", plugins);
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return StringUtil.merge(super.getReservedKeys(), key);
	}
}
