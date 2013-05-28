package com.webbuilder.interact;

import java.sql.ResultSet;
import java.sql.Types;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.webbuilder.common.Str;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class BBS {
	public static void convertChar(HttpServletRequest request,
			HttpServletResponse response) {
		String key = (String) request.getAttribute("key");
		if (key == null)
			key = "CONTENT";
		request.setAttribute(key, request.getParameter(key).replace("\u200b",
				""));
	}

	public static void createRobotPage(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ResultSet rs = (ResultSet) request.getAttribute("query1");
		StringBuilder buf = new StringBuilder();
		String pageStr = request.getParameter("page");
		int page, begin, end, index = 0, count = 0;
		boolean isBreaked = false;

		if (pageStr == null || !StringUtil.isNumeric(pageStr, false))
			page = 0;
		else
			page = Integer.parseInt(pageStr);
		begin = page * 200 + 1;
		end = (page + 1) * 200;
		buf
				.append("<!DOCTYPE html>\n<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\">\n<title>Web Development Forum</title>\n</head>\n<body>\n<H1><a href=\"http://www.putdb.com\" target=\"_blank\">");
		buf.append(Str.format(request, "webDev"));
		buf.append("</a></H1>\n<table cellpadding=\"5px\">\n");
		while (rs.next()) {
			index++;
			if (index < begin)
				continue;
			if (index > end) {
				isBreaked = true;
				break;
			}
			if (count > 0)
				buf.append("\n");
			count++;
			buf.append("<tr><td>");
			buf.append(count);
			buf
					.append("</td><td><a href=\"http://www.putdb.com/main?xwl=browse&id=");
			buf.append(rs.getString(2));
			buf.append("\" target=\"_blank\">");
			buf.append(StringUtil.toHTML(rs.getString(3)));
			buf.append("</a></td><td>");
			buf.append(rs.getTimestamp(4).toString().substring(0, 16));
			buf.append("</td></tr>");
		}
		buf.append("\n</table>\n");
		if (page > 0) {
			buf.append("<a href=\"http://www.putdb.com/main?xwl=forum&page=");
			buf.append(page - 1);
			buf.append("\" target=\"_self\">Previous Page</a>");
		}
		if (isBreaked) {
			if (page > 0)
				buf.append("&nbsp;&nbsp;");
			buf.append("<a href=\"http://www.putdb.com/main?xwl=forum&page=");
			buf.append(page + 1);
			buf.append("\" target=\"_self\">Next Page</a>");
		}
		if (page > 0 || isBreaked)
			buf.append("\n");
		buf
				.append("<h2><a href=\"http://www.putdb.com/main?xwl=portal&type=1\" target=\"_blank\">");
		buf.append(Str.format(request, "webDevPlatform"));
		buf.append("</a>&nbsp;&nbsp;");
		buf
				.append("<a href=\"http://www.putdb.com/main?xwl=portal&type=2\" target=\"_blank\">");
		buf.append(Str.format(request, "webDevTool"));
		buf.append("</a>&nbsp;&nbsp;");
		buf
				.append("<a href=\"http://www.putdb.com/main?xwl=portal&type=3\" target=\"_blank\">");
		buf.append(Str.format(request, "rapidWebDev"));
		buf.append("</a></h2>");
		buf.append("\n</body>\n</html>");
		WebUtil.response(response, buf);
	}

	public static void getPage(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Object createDate = request.getAttribute("titleQuery.USER_DATE");
		if (createDate == null || createDate.toString().isEmpty()) {
			WebUtil.response(response, "Not found.");
			return;
		}
		ResultSet postRs = (ResultSet) request.getAttribute("postQuery");
		ResultSet scoreRs = (ResultSet) request.getAttribute("scoreQuery");
		StringBuilder buf = new StringBuilder();
		int i = 0, score;
		String title, space = "&nbsp;&nbsp;", sn = request.getParameter("sn");
		boolean outOfPanel = request.getParameter("type") == null;
		int levels[] = { 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000 };
		String delKey, divId, roles[];
		String userLabel = Str.format(request, "username") + ": ", dateLabel = "  "
				+ Str.format(request, "createDate") + ": ";
		HttpSession session = request.getSession(false);
		if (session == null)
			roles = null;
		else
			roles = (String[]) session.getAttribute("sys.userRoles");
		if (!outOfPanel && StringUtil.indexOf(roles, "admin") != -1)
			delKey = Str.format(request, "deleteStr");
		else
			delKey = null;
		title = getText(request.getAttribute("titleQuery.TITLE"), true);
		if (outOfPanel)
			buf.append(getHeader(request));
		buf.append("<div");
		if (!outOfPanel)
			buf.append(" style=\"line-height:2;padding:8px 20px 8px 20px;\"");
		buf.append(" class=\"wb_normal\">");
		buf.append("<div id=\"bv__");
		buf.append(sn);
		buf.append("_0\"><p class=\"bg\"><span title=");
		buf.append(StringUtil.quote(StringUtil.concat(userLabel, getText(
				request.getAttribute("titleQuery.USER_NAME"), true), dateLabel,
				createDate.toString().substring(0, 10))));
		buf.append(">");
		buf.append(getText(request.getAttribute("titleQuery.DISPLAY_NAME"),
				true));
		buf.append("</span>");
		buf.append(space);
		buf.append("<img class=\"lv");
		if (scoreRs.next())
			score = scoreRs.getInt(1);
		else
			score = 0;
		buf.append(getLevel(levels, score));
		buf.append("\" src=\"webbuilder/images/app/s.gif\">");
		buf.append(space);
		buf.append("<span id=\"tl__");
		buf.append(sn);
		buf.append("_0\">");
		buf
				.append(getText(request.getAttribute("titleQuery.CREATE_DATE"),
						true));
		buf
				.append("</span></p><p class=\"wb_title\" style=\"margin:8px 0px 8px 0px\">");
		buf.append(title);
		buf.append("</p><div>");
		buf.append(getText(request.getAttribute("titleQuery.CONTENT"), false));
		buf.append("</div></div>");
		while (postRs.next()) {
			i++;
			divId = "bv__" + sn + "_" + i;
			buf.append("<div id=\"");
			buf.append(divId);
			buf.append("\">");
			buf.append("<div class=\"wb_line\"></div>");
			buf.append("<p class=\"bg\">#");
			buf.append(i);
			buf.append(space);
			buf.append("<span title=");
			buf.append(StringUtil.quote(StringUtil.concat(userLabel, getText(
					postRs.getString(1), true), dateLabel, postRs.getTimestamp(
					2).toString().substring(0, 10))));
			buf.append(">");
			buf.append(getText(postRs.getString(3), true));
			buf.append("</span>");
			buf.append(space);
			buf.append("<img class=\"lv");
			buf.append(getLevel(levels, postRs.getInt(4)));
			buf.append("\" src=\"webbuilder/images/app/s.gif\">");
			buf.append(space);
			buf.append("<span id=\"tl__");
			buf.append(sn);
			buf.append("_");
			buf.append(i);
			buf.append("\">");
			buf.append(getText(postRs.getTimestamp(5), true));
			buf.append("</span>");
			if (delKey != null)
				buf.append(getDelHtml(divId, postRs.getString(6), delKey));
			buf.append("</p><div>");
			buf.append(getText(DbUtil.getObject(postRs, 7, Types.LONGVARCHAR),
					false));
			buf.append("</div></div>");
		}
		buf.append("</div>");
		if (outOfPanel)
			buf.append("</body></html>");
		WebUtil.response(response, buf);
	}

	private static String getHeader(HttpServletRequest request)
			throws Exception {
		StringBuilder sb = new StringBuilder();
		sb
				.append("<!DOCTYPE html><html><head><meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"><title>Web Development Forum</title><link rel=\"stylesheet\" href=\"webbuilder/css/style.css\" type=\"text/css\">");
		sb.append("<style type=\"text/css\">");
		sb.append(".top-row .x-grid-cell{background-color:#F0F0F0}");
		sb
				.append(".bg{background-color:#F5F5F5;border:solid;border-color:#F3F3F3;white-space:nowrap;}");
		sb
				.append(".lv1{width:12px;height:9px;background-image:url(webbuilder/images/app/angle.gif)}");
		sb
				.append(".lv2{width:24px;height:9px;background-image:url(webbuilder/images/app/angle.gif)}");
		sb
				.append(".lv3{width:36px;height:9px;background-image:url(webbuilder/images/app/angle.gif)}");
		sb
				.append(".lv4{width:48px;height:9px;background-image:url(webbuilder/images/app/angle.gif)}");
		sb
				.append(".lv5{width:60px;height:9px;background-image:url(webbuilder/images/app/angle.gif)}");
		sb
				.append(".lv6{width:12px;height:9px;background-image:url(webbuilder/images/app/star.gif)}");
		sb
				.append(".lv7{width:24px;height:9px;background-image:url(webbuilder/images/app/star.gif)}");
		sb
				.append(".lv8{width:36px;height:9px;background-image:url(webbuilder/images/app/star.gif)}");
		sb
				.append(".lv9{width:48px;height:9px;background-image:url(webbuilder/images/app/star.gif)}");
		sb
				.append(".lv10{width:60px;height:9px;background-image:url(webbuilder/images/app/star.gif)}");
		sb.append("</style>");
		sb
				.append("</head><body style=\"line-height:2;padding:8px 20px 8px 20px;\"><p><a href=\"http://www.putdb.com\">");
		sb.append(Str.format(request, "homePage"));
		sb.append("</a></p>");
		return sb.toString();
	}

	private static String getDelHtml(String divId, String id, String key) {
		StringBuilder buf = new StringBuilder();

		buf.append("&nbsp;&nbsp;<a href=\"javascript:delPost('");
		buf.append(divId);
		buf.append("','");
		buf.append(id);
		buf.append("')\" class=\"wb_link\">");
		buf.append(key);
		buf.append("</a>");
		return buf.toString();
	}

	private static int getLevel(int[] levels, int score) {
		int i, j = levels.length;

		for (i = j - 1; i >= 0; i--)
			if (score > levels[i])
				return i + 2;
		return 1;
	}

	private static String getText(Object obj, boolean encodeHtml) {
		if (obj instanceof java.sql.Timestamp)
			return obj.toString().substring(0, 19);
		else if (obj == null)
			return "";
		else
			return encodeHtml ? StringUtil.toHTML(obj.toString()) : obj
					.toString();
	}
}
