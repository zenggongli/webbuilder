package com.webbuilder.controls;

import java.sql.ResultSet;

import com.webbuilder.common.SysMap;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.StringUtil;

public class ComboBox extends ExtField {
	private String key[] = { "store", "pickList", "query", "listConfig",
			"forceList", "listWidth", "listResizable", "minChars", "queryLike",
			"beforequery", "validator", "keyName", "keySortType" };

	protected String getTagProperties() throws Exception {
		super.getTagProperties();
		String store = gs("store"), pickList = gs("pickList"), minChars = gs("minChars"), query = gs("query");
		String listWidth = gs("listWidth"), validator = gs("validator"), keyName = gs("keyName");
		boolean resize = gb("listResizable", false), forceList = gb(
				"forceList", false);

		if (!keyName.isEmpty()) {
			pickList = SysMap.getList(keyName, gs("keySortType"));
			forceList = true;
		}
		if (!StringUtil.isEmpty(pickList))
			store = pickList;
		else if (!StringUtil.isEmpty(query))
			store = DbUtil.getArray((ResultSet) ga(query));
		if (minChars.isEmpty())
			minChars = "0";
		addExpress("minChars", minChars);
		addExpress("store", store);
		setObjects(gs("listConfig"));
		if (resize)
			addObject("resizable", "{handles:'se',transparent:true}");
		if (validator.isEmpty()) {
			if (forceList)
				validator = "Wb.listValidator";
		} else
			validator = StringUtil.concat("function(value){\n", validator,
					"\n}");
		addExpress("forceList", Boolean.toString(forceList));
		addExpress("validator", validator);
		addObject("width", listWidth);
		addExpress("listConfig", getOBuffer());
		if (!StringUtil.isEmpty(listWidth))
			addExpress("matchFieldWidth", "false");
		return getPBuffer();
	}

	protected String getTagEvents() throws Exception {
		String beforequery = gs("beforequery");

		if (beforequery.isEmpty()) {
			if (gb("queryLike", false))
				addEvent("beforequery", "Wb.like");
		} else
			addEvent("beforequery", StringUtil.concat(
					"function(queryEvent,options){\n", beforequery, "\n}"));
		return getEBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return StringUtil.merge(super.getReservedKeys(), key);
	}
}
