package com.webbuilder.common;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.StringUtil;

public class Str {
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, String>> langs;
	private static ConcurrentHashMap<String, String> extLangList;

	public static String format(String key, String... args) throws Exception {
		return langFormat(Var.get("webbuilder.defaultLanguage"), key, args);
	}

	public static String format(HttpServletRequest request, String key,
			String... args) throws Exception {
		return langFormat((String) request.getAttribute("sys.lang"), key, args);
	}

	private static String optLang(ConcurrentHashMap<String, ?> map, String lang)
			throws Exception {
		if (map == null)
			initialize(false);
		if (!StringUtil.isEmpty(lang)) {
			if (map.containsKey(lang))
				return lang;
			int pos = lang.indexOf('_');
			if (pos != -1) {
				lang = lang.substring(0, pos);
				if (map.containsKey(lang))
					return lang;
			}
		}
		return Var.get("webbuilder.defaultLanguage");
	}

	public static String optLanguage(String lang) throws Exception {
		return optLang(langs, lang);
	}

	public static String optExtLanguage(String lang) throws Exception {
		return optLang(extLangList, lang);
	}

	public static String langFormat(String lang, String key, String... args)
			throws Exception {
		if (langs == null)
			initialize(false);
		ConcurrentHashMap<String, String> buffer = langs.get(optLanguage(lang));
		if (buffer == null)
			return key;
		String str = buffer.get(key);
		if (str == null)
			return key;
		int i = 0;
		for (String s : args)
			str = StringUtil.replace(str, "{" + (i++) + "}", s);
		return str;
	}

	public static ConcurrentHashMap<String, ConcurrentHashMap<String, String>> getLangMap()
			throws Exception {
		if (langs == null)
			initialize(false);
		return langs;
	}

	public static synchronized void initialize(boolean reload) throws Exception {
		if (!reload && langs != null)
			return;
		langs = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
		ConcurrentHashMap<String, String> buffer;
		File[] fs = new File(Main.path, "webbuilder/script/locale").listFiles();
		JSONObject jo;
		String name, json, ns[];

		for (File f : fs) {
			name = f.getName();
			if (StringUtil.isSame(StringUtil.substring(name, 0, 7), "wb-lang")) {
				json = FileUtil.readUtfText(f);
				buffer = new ConcurrentHashMap<String, String>();
				if (!StringUtil.isEmpty(json)) {
					jo = new JSONObject(json.substring(json.indexOf('{')));
					ns = JSONObject.getNames(jo);
					for (String n : ns)
						buffer.put(n, jo.getString(n));
				}
				langs.put(name.substring(8, name.length() - 3), buffer);
			}
		}
		extLangList = new ConcurrentHashMap<String, String>();
		fs = new File(Main.path, "webbuilder/controls/ext/locale").listFiles();
		for (File f : fs) {
			name = f.getName();
			if (StringUtil.isSame(StringUtil.substring(name, 0, 8), "ext-lang")) {
				name = name.substring(9, name.length() - 3);
				extLangList.put(name, name);
			}
		}
	}
}
