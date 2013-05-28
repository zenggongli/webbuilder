package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Grid extends ExtControl {
	private String key[] = { "columns", "multiSelect", "bbar", "autoLoad",
			"plugins", "pluginType", "features", "viewConfig", "pagingToolbar",
			"exportExcel", "pageSizeMenu", "exportPrint" };

	protected String getTagProperties() throws Exception {
		String cols = gs("columns"), bbar = gs("bbar"), store = gs("store");
		String plugins = gs("plugins"), pluginType = gs("pluginType"), viewConfig = gs("viewConfig");
		String features = gs("features"), featureType = gs("featureType");
		boolean mul = gb("multiSelect", true), pg = gb("pagingToolbar", true);

		if (StringUtil.isEmpty(cols))
			cols = "[]";
		addExpress("columns", cols);
		if (plugins.isEmpty()) {
			if (pluginType.equals("cellediting"))
				plugins = "[{ptype:\"cellediting\",clicksToEdit:1}]";
			else if (pluginType.equals("rowediting"))
				plugins = "[{ptype:\"rowediting\",clicksToEdit:1}]";
		}
		addExpress("plugins", plugins);
		if (features.isEmpty()) {
			if (featureType.equals("grouping"))
				features = "[{ftype:\"grouping\",groupHeaderTpl:\"{name}\"}]";
			else if (featureType.equals("summary"))
				features = "[{ftype:\"summary\"}]";
			else if (featureType.equals("groupingsummary"))
				features = "[{ftype:\"groupingsummary\",groupHeaderTpl:\"{name}\"}]";
		}
		addExpress("features", features);
		addExpress("viewConfig", viewConfig);
		if (mul)
			addExpress("multiSelect", "true");
		if (StringUtil.isEmpty(bbar) && pg)
			bbar = StringUtil.concat("Wb.getPagingBar(",
					store.isEmpty() ? "null" : store, ",\"", gs("id"), "\",",
					gb("exportExcel", true) ? "1" : "0", ",", gb(
							"pageSizeMenu", true) ? "1" : "0", ",", gb(
							"exportPrint", true) ? "1" : "0", ")");
		addExpress("bbar", bbar);
		if (!store.isEmpty() && gb("autoLoad", true))
			finalScript(StringUtil.concat("Wb.load(", store, ");"));
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}