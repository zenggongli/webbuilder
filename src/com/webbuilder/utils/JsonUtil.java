package com.webbuilder.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtil {
	public static HashMap<JSONObject, JSONObject> getRelations(
			JSONArray jsonArray, ArrayList<JSONObject> children, String key)
			throws Exception {
		HashMap<JSONObject, JSONObject> map = new HashMap<JSONObject, JSONObject>();
		markParent(map, null, jsonArray, children, key);
		return map;
	}

	private static void markParent(HashMap<JSONObject, JSONObject> map,
			JSONObject parent, JSONArray jsonArray,
			ArrayList<JSONObject> children, String key) throws Exception {
		int i, j = jsonArray.length();
		JSONObject jo;
		JSONArray ja;

		for (i = 0; i < j; i++) {
			jo = jsonArray.getJSONObject(i);
			if (parent != null)
				map.put(jo, parent);
			ja = jo.optJSONArray(key);
			if (ja == null)
				children.add(jo);
			else
				markParent(map, jo, ja, children, key);
		}
	}

	public static String insert(JSONArray ja, String text, int index)
			throws Exception {
		StringBuilder sb = new StringBuilder();
		int i, j = ja.length();

		sb.append('[');
		for (i = 0; i < index; i++) {
			sb.append(ja.get(i).toString());
			sb.append(',');
		}
		sb.append(text);
		for (i = index; i < j; i++) {
			sb.append(',');
			sb.append(ja.get(i).toString());
		}
		sb.append(']');
		return sb.toString();
	}

	public static JSONObject readObject(File file) throws Exception {
		String s = FileUtil.readUtfText(file);
		if (StringUtil.isEmpty(s))
			return new JSONObject();
		else
			return new JSONObject(s.substring(s.indexOf('{')));
	}

	public static JSONArray readArray(File file) throws Exception {
		String s = FileUtil.readUtfText(file);
		if (StringUtil.isEmpty(s))
			return new JSONArray();
		else
			return new JSONArray(s.substring(s.indexOf('[')));
	}

	public static String optString(JSONObject jo, String key) {
		if (jo.isNull(key))
			return "";
		else
			return jo.optString(key);
	}

	public static String optString(JSONArray ja, int index) {
		if (ja.isNull(index))
			return "";
		else
			return ja.optString(index);
	}

	public static Object opt(JSONObject jo, String key) {
		if (jo.isNull(key))
			return null;
		else
			return jo.opt(key);
	}

	public static Object opt(JSONArray ja, int index) {
		if (ja.isNull(index))
			return null;
		else
			return ja.opt(index);
	}

	public static void clear(JSONObject jo) throws Exception {
		Iterator<?> t = jo.keys();
		while (t.hasNext()) {
			jo.put((String) t.next(), "");
		}
	}

	public static String getText(ArrayList<String> list) {
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;

		buf.append('[');
		for (String s : list) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(',');
			buf.append(StringUtil.quote(s));
		}
		buf.append(']');
		return buf.toString();
	}

	public static JSONObject findObject(JSONArray ja, String key, String text)
			throws Exception {
		int i, j = ja.length();
		JSONObject jo;

		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			if (optString(jo, key).equals(text))
				return jo;
		}
		return null;
	}
}
