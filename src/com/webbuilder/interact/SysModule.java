package com.webbuilder.interact;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.webbuilder.common.Resource;
import com.webbuilder.common.Str;
import com.webbuilder.common.Value;
import com.webbuilder.common.Var;
import com.webbuilder.tool.Encrypter;
import com.webbuilder.utils.StringUtil;

public class SysModule {
	public static void getRegPwd(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String pwd = request.getParameter("pwd"), key = "^%%rd%%^", user = request
				.getParameter("regName");

		if (user.indexOf('"') != -1 || user.indexOf('\'') != -1)
			throw new Exception(Str.format(request, "invalidName", user));
		if (pwd == null || pwd.length() < 6)
			throw new Exception(Str.format(request, "invalidPwdLen"));
		request.setAttribute("pwd", Encrypter.getMD5(pwd));
		if (pwd.equals(key))
			throw new Exception(Str.format(request, "invalidPwd"));
	}

	public static void checkRegVC(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (!Var.getBool("webbuilder.allowRegister"))
			throw new Exception("Not allowed");
		HttpSession session = request.getSession(false);
		if (session == null)
			throw new Exception(Str.format(request, "vcExpired"));
		String vcCode = (String) session.getAttribute("sys.verifyCode");
		if (StringUtil.isEmpty(vcCode)
				|| !StringUtil.isSame(vcCode, request.getParameter("regVC"))) {
			throw new Exception(Str.format(request, "invalidVc"));
		}
		session.removeAttribute("sys.verifyCode");
	}

	public static void saveDesktop(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String data = request.getParameter("data");

		Resource.set(request, "wb.desktop", data);
	}

	public static void initIndex(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		checkRegister(request, response);
		if (Var.getBool("webbuilder.app.index.saveLastPath"))
			request.setAttribute("indexPath", StringUtil.quote(Value.get(
					request, "wb.index.path")));
		else
			request.setAttribute("indexPath", "\"-\"");
		String data = Resource.get(request, "wb.desktop");
		if (StringUtil.isEmpty(data))
			data = Resource.get("wb.desktop.default");
		if (StringUtil.isEmpty(data))
			data = "null";
		request.setAttribute("desktop", data);
	}

	public static void checkRegister(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		request.setAttribute("registered", Install.isRegistered());
	}

	public static void savePortalPath(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (Var.getBool("webbuilder.app.index.saveLastPath")) {
			String path = request.getParameter("path");
			if (!StringUtil.isEmpty(path))
				Value.set(request, "wb.index.path", path);
		}
	}
}
