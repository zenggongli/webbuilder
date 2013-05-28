package com.webbuilder.interact;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Main;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Docs {
	public static void searchKey(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		StringBuilder buf = new StringBuilder();
		String key;
		int times = 0;

		buf.append("[");
		key = request.getParameter("query");
		if (!StringUtil.isEmpty(key))
			recurseSearch(buf,
					new File(Main.path, "webbuilder/docs/index.txt"), key,
					times);
		buf.append("]");
		WebUtil.response(response, buf);
	}

	private static int recurseSearch(StringBuilder buf, File idxFile,
			String key, int times) throws Exception {
		JSONArray ja = JsonUtil.readArray(idxFile);
		JSONObject jo;
		File file;
		String content, fileName, text, str, iconCls;
		char c;
		int i, j = ja.length(), k, x, bx, ex, l = key.length(), y, contentLen;

		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			text = JsonUtil.optString(jo, "text");
			fileName = JsonUtil.optString(jo, "file");
			iconCls = JsonUtil.optString(jo, "icon");
			if (iconCls.isEmpty())
				iconCls = "ht_icon";
			if (fileName.isEmpty())
				fileName = text;
			file = new File(idxFile.getParentFile(), fileName);
			if (!file.exists())
				continue;
			if (file.isDirectory()) {
				file = new File(file, "index.txt");
				if (file.exists())
					times = recurseSearch(buf, file, key, times);
			} else {
				content = FileUtil.readUtfText(file);
				contentLen = content.length();
				k = content.toLowerCase().indexOf(key.toLowerCase());
				if (k != -1) {
					if (times > 0)
						buf.append(",");
					buf.append("{file:");
					buf.append(StringUtil.quote(FileUtil.getPath(file)));
					buf.append(",iconCls:");
					buf.append(StringUtil.quote(iconCls));
					buf.append(",title:");
					buf.append(StringUtil.quote(text));
					buf.append(",text:\"<b>");
					buf.append(StringUtil.toHTML(text));
					buf.append("</b><br>");
					bx = k;
					for (x = 1; x < 50; x++) {
						y = k - x;
						if (y < 0)
							break;
						c = content.charAt(y);
						if (c == '<' || c == '>' || x > 20
								&& (c == ' ' || c == ',' || c == '.'))
							break;
						bx = y;
					}
					ex = k + l;
					for (x = 0; x < 50; x++) {
						y = k + l + x;
						if (y >= contentLen)
							break;
						ex = y;
						c = content.charAt(y);
						if (c == '<' || c == '>' || x > 20
								&& (c == ' ' || c == ',' || c == '.'))
							break;
					}
					str = (content.substring(bx, k) + "*").trim();
					buf.append(StringUtil.toHTML(str.substring(0,
							str.length() - 1)));
					buf.append("<span style='background-color:#FF0'>");
					buf.append(StringUtil.toHTML(content.substring(k, k + l)));
					buf.append("</span>");
					y = k + l;
					if (ex < contentLen && y < contentLen)
						buf.append(StringUtil.toHTML(content.substring(y, ex)));
					buf.append("\"}");
					if (times > 29)
						break;
					times++;
				}
			}
		}
		return times;
	}

	public static void getFileContent(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String dir = request.getParameter("dir");

		if (dir.startsWith("@"))
			WebUtil.response(response, FileUtil.readUtfText(new File(Main.path,
					"webbuilder/docs/" + dir.substring(1))));
		else
			WebUtil.response(response, FileUtil.readUtfText(new File(dir)));
	}

	public static void getTopicTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String link, dir = request.getParameter("dir"), iconCls, fileName, text;
		StringBuilder buf = new StringBuilder();
		File base, file;
		JSONArray ja;
		JSONObject jo;
		int i, j;
		boolean isFirst = true, isLink;

		if (StringUtil.isEmpty(dir))
			base = new File(Main.path, "webbuilder/docs");
		else
			base = new File(dir);
		file = new File(base, "index.txt");
		ja = JsonUtil.readArray(file);
		buf.append("[");
		j = ja.length();
		for (i = 0; i < j; i++) {
			jo = ja.getJSONObject(i);
			text = jo.optString("text");
			fileName = JsonUtil.optString(jo, "file");
			link = JsonUtil.optString(jo, "link");
			isLink = !link.isEmpty();
			if (fileName.isEmpty())
				fileName = text;
			file = new File(base, fileName);
			if (!file.exists() && !isLink)
				continue;
			if (isFirst)
				isFirst = false;
			else
				buf.append(",");
			buf.append("{text:");
			buf.append(StringUtil.quote(text));
			buf.append(",dir:");
			buf.append(StringUtil.quote(FileUtil.getPath(file)));
			if (isLink) {
				buf.append(",link:");
				buf.append(StringUtil.quote(link));
			}
			iconCls = JsonUtil.optString(jo, "icon");
			if (!iconCls.isEmpty()) {
				buf.append(",iconCls:");
				buf.append(StringUtil.quote(iconCls));
			} else if (!file.isDirectory())
				buf.append(",iconCls:\"ht_icon\"");
			if (file.isFile() || isLink)
				buf.append(",leaf:true");
			else if (!FileUtil.hasSubFile(file, false))
				buf.append(",children:[]");
			buf.append("}");
		}
		buf.append("]");
		WebUtil.response(response, buf);
	}
}
