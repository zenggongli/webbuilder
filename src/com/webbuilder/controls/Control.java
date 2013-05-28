package com.webbuilder.controls;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Control {
	public HttpServletRequest request;
	public HttpServletResponse response;
	public JSONObject xwlObject;
	public JSONObject parentControl;
	public boolean hasChild;
	public boolean hasParent;
	public boolean isFirstChild;
	public String xwlId;
	private JSONObject xwlMeta;
	private JSONObject propertyList;

	public void create() throws Exception {
	}

	public void setXwlMeta(JSONObject jo) {
		xwlMeta = jo;
	}

	public void setPropertyList(JSONObject jo) {
		propertyList = jo;
	}

	protected boolean isProperty(String name) {
		return propertyList != null && propertyList.has(name);
	}

	protected String gs(String name) throws Exception {
		return StringUtil.replaceParameters(request, xwlObject.optString(name));
	}

	protected int gi(String name, int defaultValue) throws Exception {
		String s = gs(name);
		if (StringUtil.isEmpty(s))
			return defaultValue;
		else
			return Integer.parseInt(s);
	}

	protected boolean gb(String name, boolean defaultVal) throws Exception {
		String v = gs(name);

		if (StringUtil.isEmpty(v))
			return defaultVal;
		else
			return StringUtil.getBool(v);
	}

	protected String gp(String name) throws Exception {
		return WebUtil.fetch(request, name);
	}

	protected Object ga(String name) throws Exception {
		Object obj = request.getAttribute(name);
		if (obj == null)
			return request.getParameter(name);
		else
			return obj;
	}

	protected String getMeta(String prop) {
		return xwlMeta.optString(prop);
	}
}