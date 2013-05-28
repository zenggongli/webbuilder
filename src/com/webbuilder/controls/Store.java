package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Store extends ExtControl {
	private String key[] = { "id", "url", "fields", "beforeload", "load",
			"exception", "proxyType", "timeout", "mask", "showMask", "message",
			"showResult", "pageSize", "params" };
	private boolean isTree;

	public void create() throws Exception {
		String type = getMeta("xwlType");
		isTree = type.indexOf("TreeStore") != -1;
		headerScript(StringUtil.concat("Wd.", gs("id"), "=new ", type, "({"));
		footerScript("});");
		headerScript(getJson(null));
	}

	protected String getTagProperties() throws Exception {
		String except, proxy, proxyType, fields = gs("fields"), pageSize = gs("pageSize"), params = gs("params"), timeout = gs("timeout");

		proxy = gs("proxy");
		if (proxy.isEmpty()) {
			proxyType = gs("proxyType");
			if (proxyType.isEmpty())
				proxyType = "ajax";
			addText("type", proxyType);
			addText("url", WebUtil.getUrl(gs("url"), false));
			if (timeout.equals("-1"))
				timeout = "Wb.maxInt";
			addExpress("timeout", timeout);
			if (!isTree)
				addExpress("reader",
						"{type:\"json\",root:\"rows\",totalProperty:\"total\"}");
			except = gs("exception");
			if (gb("showResult", true)) {
				if (except.isEmpty())
					except = "Wb.except(response.responseText);";
				else
					except = except + "\nWb.except(response.responseText);";
			}
			if (!except.isEmpty())
				addExpress(
						"listeners",
						StringUtil
								.concat(
										"{exception:function(proxy,response,operation,options){\n",
										except, "\n}}"));
			proxy = getPBuffer();
			resetPBuffer();
			addExpress("proxy", "{" + proxy + "}");
		} else
			addExpress("proxy", proxy);
		if (!isTree && fields.isEmpty())
			fields = "[]";
		addExpress("fields", fields);
		if (pageSize.equals("-1"))
			pageSize = "Wb.maxInt";
		addExpress("pageSize", pageSize);
		if (params.isEmpty())
			params = "{}";
		addExpress("params", params);
		return getPBuffer();
	}

	protected String getTagEvents() throws Exception {
		String loadFuncPara, beforeload = gs("beforeload"), msk = ge("mask"), msg = ge("msg"), load = gs("load");
		boolean showMask = gb("showMask", !isTree), buffered = gb("buffered",
				false);

		if (!load.isEmpty())
			load = StringUtil.concat("if(successful){\n", load, "\n}");
		if (isTree)
			loadFuncPara = "store,node,records,successful,options";
		else
			loadFuncPara = "store,records,successful,operation,options";
		if (!gs("grid").isEmpty()) {
			String f = StringUtil.concat("if(successful)Wb.setGrid(",
					ge("grid"), ",this);");
			if (load.isEmpty())
				load = f;
			else
				load = StringUtil.concat(f, "\n", load);
		}
		if (beforeload.isEmpty())
			beforeload = "Wb.setStore(store);";
		else
			beforeload = StringUtil.concat("Wb.setStore(store);\n", beforeload);
		if (showMask) {
			if (load.isEmpty())
				load = StringUtil.concat("Wb.unmask(", msk, ");");
			else
				load = StringUtil.concat("Wb.unmask(", msk, ");\n", load);
			msk = StringUtil.concat("Wb.mask(", msk, ",", msg, ");");
			beforeload = StringUtil.concat(beforeload, "\n", msk);
		}
		if (!StringUtil.isEmpty(beforeload))
			addEvent(buffered ? "beforeprefetch" : "beforeload", StringUtil
					.concat("function(store,operation,options){\n", beforeload,
							"\n}"));
		if (!StringUtil.isEmpty(load))
			addEvent(buffered ? "prefetch" : "load", StringUtil.concat(
					"function(", loadFuncPara, "){\n", load, "\n}"));
		return getEBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}