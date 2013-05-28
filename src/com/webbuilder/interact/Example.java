package com.webbuilder.interact;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.webbuilder.common.Main;
import com.webbuilder.common.Str;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Example {
	public static void setLanContent(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Enumeration<?> enums = request.getParameterNames();
		String p, k, lan, content;
		ConcurrentHashMap<String, String> lm;
		ConcurrentHashMap<String, ConcurrentHashMap<String, String>> map = Str
				.getLangMap();
		JSONObject jo;
		Iterator<?> it;
		File f;

		while (enums.hasMoreElements()) {
			p = enums.nextElement().toString();
			if (StringUtil.isEqual(StringUtil.substring(p, 0, 5), "lang_")) {
				lan = p.substring(5);
				content = request.getParameter(p);
				f = new File(Main.path, "webbuilder/script/locale/wb-lang-"
						+ lan + ".js");
				try {
					jo = new JSONObject(StringUtil.concat("{", content, "}"));
				} catch (Throwable e) {
					throw new Exception(lan + ": "
							+ Str.format(request, "invalidFormat"));
				}
				FileUtil.writeUtfText(f, StringUtil.concat("var Str={\n",
						content, "\n}"));
				lm = new ConcurrentHashMap<String, String>();
				it = jo.keys();
				while (it.hasNext()) {
					k = (String) it.next();
					lm.put(k, jo.getString(k));
				}
				map.put(lan, lm);
			}
		}
	}

	public static void langEditorInit(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		StringBuilder tabs = new StringBuilder(), tree = new StringBuilder();
		ConcurrentHashMap<String, ConcurrentHashMap<String, String>> lans = Str
				.getLangMap();
		List<Entry<String, ?>> ls = StringUtil.sortMapKey(lans);
		ConcurrentHashMap<String, String> v;
		String key;
		boolean isFirst = true;

		for (Entry<String, ?> e : ls) {
			key = e.getKey();
			v = lans.get(key);
			if (v.size() != 0) {
				if (isFirst)
					isFirst = false;
				else {
					tabs.append(',');
					tree.append(',');
				}
				tabs.append("{title:");
				tabs.append(StringUtil.quote(key));
				tabs
						.append(StringUtil
								.concat(
										",id:\"tab_",
										key,
										"\",layout:\"fit\",items:[{xtype:\"textarea\",id:\"area_",
										key, "\"}]"));
				tabs.append('}');
				tree.append("{text:");
				tree.append(StringUtil.quote(key));
				tree.append(",iconCls:\"item_icon\",leaf:true");
				tree.append('}');
			}
		}
		request.setAttribute("langTabs", tabs.toString());
		request.setAttribute("treeContent", tree.toString());
	}

	public static void getLanContent(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		File f = new File(Main.path, "webbuilder/script/locale/wb-lang-"
				+ request.getParameter("lang") + ".js");
		String n, s = FileUtil.readUtfText(f);
		if (StringUtil.isEmpty(s))
			WebUtil.response(response, "");
		else {
			JSONObject jo = new JSONObject(s.substring(s.indexOf('{')));
			StringBuilder buf = new StringBuilder();
			Iterator<?> it = jo.sortedKeys();
			boolean isFirst = true;

			while (it.hasNext()) {
				n = (String) it.next();
				if (isFirst)
					isFirst = false;
				else
					buf.append(",\n");
				buf.append(n);
				buf.append(" : ");
				buf.append(StringUtil.quote(jo.getString(n)));
			}
			WebUtil.response(response, buf);
		}
	}
}
