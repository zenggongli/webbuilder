package com.webbuilder.common;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.webbuilder.interact.Install;
import com.webbuilder.tool.Encrypter;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Session {
	public static final ConcurrentHashMap<String, HttpSession> sessionList = new ConcurrentHashMap<String, HttpSession>();

	public static void verify(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String referer = request.getHeader("Referer"), userInfo[];
		HttpSession session = request.getSession(false);
		int userCt;
		if (StringUtil.isEmpty(referer)
				|| referer.indexOf("main?xwl=login") != -1)
			referer = "main?xwl=index";
		if (session != null
				&& session.getAttribute("sys.logined") != null
				&& StringUtil.isEqual(request.getParameter("username"),
						(String) session.getAttribute("sys.userName"))) {
			WebUtil.response(response, referer);
			request.setAttribute("ignore", true);
			return;
		}
		if (Var.getBool("webbuilder.session.loginVerify"))
			checkVerifyCode(request);
		userCt = Install.getUserCount();
		if (userCt != -1 && sessionList.size() > userCt - 1)
			if (userCt == 3)
				throw new Exception("Trial version only allows 3 active users.");
			else
				throw new Exception("The license only allows " + userCt
						+ " active users.");
		userInfo = checkUser(request);
		createSession(request, userInfo);
		WebUtil.response(response, referer);
	}

	public static void logout(HttpServletRequest request,
			HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null)
			session.invalidate();
	}

	private static void checkVerifyCode(HttpServletRequest request)
			throws Exception {
		HttpSession session = request.getSession(false);
		if (session == null)
			throw new Exception(Str.format(request, "vcExpired"));
		String vcCode = (String) session.getAttribute("sys.verifyCode");
		session.removeAttribute("sys.verifyCode");
		if (StringUtil.isEmpty(vcCode)
				|| !StringUtil.isSame(vcCode, request
						.getParameter("verifyCode"))) {
			throw new Exception(Str.format(request, "invalidVc"));
		}
	}

	private static String[] checkUser(HttpServletRequest request)
			throws Exception {
		ResultSet rs = DbUtil
				.query(
						request,
						"select USER_ID,USER_NAME,DISPLAY_NAME,PASSWORD,USE_LANG from WB_USER where USER_NAME={?username?} and STATUS=1");
		String password = request.getParameter("password"), username = request
				.getParameter("username"), truePwd, result[] = new String[4];

		if (!rs.next())
			throw new Exception(Str.format(request, "userNotExist", username));
		else {
			result[0] = rs.getString(1);
			result[1] = rs.getString(2);
			result[2] = rs.getString(3);
			truePwd = rs.getString(4);
			result[3] = rs.getString(5);
		}
		if (!StringUtil.isEqual(Encrypter.getMD5(password), truePwd))
			throw new Exception(Str.format(request, "invalidPwd"));
		return result;
	}

	private static String[] getRoles(HttpServletRequest request, String userId)
			throws Exception {
		request.setAttribute("userId", userId);
		ResultSet rs = DbUtil.query(request,
				"select ROLE_ID from WB_USER_ROLE where USER_ID={?userId?}");
		ArrayList<String> list = new ArrayList<String>();
		while (rs.next()) {
			list.add(rs.getString(1));
		}
		int size = list.size();
		if (size == 0)
			return null;
		else
			return list.toArray(new String[size]);
	}

	private static void createSession(HttpServletRequest request,
			String[] userInfo) throws Exception {
		int timeout = Var.getInt("webbuilder.session.sessionTimeout");
		HttpSession session = request.getSession(true);
		session.setAttribute("sys.logined", 1);
		request.setAttribute("sys.user", userInfo[0]);
		request.setAttribute("sys.userName", userInfo[1]);
		request.setAttribute("sys.dispName", userInfo[2]);
		request.setAttribute("sys.lang", userInfo[3]);
		if (timeout != 0)
			session.setMaxInactiveInterval(timeout);
		session.setAttribute("sys.user", userInfo[0]);
		session.setAttribute("sys.userName", userInfo[1]);
		session.setAttribute("sys.dispName", userInfo[2]);
		session.setAttribute("sys.lang", userInfo[3]);
		session.setAttribute("sys.userRoles", getRoles(request, userInfo[0]));
		session.setAttribute("sys.userIp", request.getRemoteAddr());
		UserInfo user = new UserInfo();
		user.setSession(session);
		session.setAttribute("sys.userInfo", user);
	}
}
