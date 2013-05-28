package com.webbuilder.common;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import com.webbuilder.utils.LogUtil;

public class UserInfo implements HttpSessionBindingListener {
	private HttpSession session;
	private String ip;
	private String userId;
	private String userName;

	public void setSession(HttpSession sess) {
		session = sess;
		ip = (String) session.getAttribute("sys.userIp");
		userId = (String) session.getAttribute("sys.user");
		userName = (String) session.getAttribute("sys.userName");
	}

	public void valueBound(HttpSessionBindingEvent event) {
		if (canLog())
			LogUtil.log(userName, ip, 1, "login");
		HttpSession sess = Session.sessionList.get(userId);
		if (sess != null && sess != session)
			sess.invalidate();
		Session.sessionList.put(userId, session);
	}

	public void valueUnbound(HttpSessionBindingEvent event) {
		if (canLog())
			LogUtil.log(userName, ip, 1, "logout");
		Session.sessionList.remove(userId);
	}

	private boolean canLog() {
		try {
			return Var.getBool("webbuilder.session.loginLog");
		} catch (Throwable e) {
			return false;
		}
	}
}
