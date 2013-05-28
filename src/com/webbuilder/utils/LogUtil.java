package com.webbuilder.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.quartz.JobExecutionContext;

import com.webbuilder.common.Var;

public class LogUtil {
	public static void log(String userName, String ip, int type, String msg) {
		try {
			if (!Var.getBool("server.log"))
				return;
		} catch (Throwable e) {
			return;
		}
		int i;
		Connection conn = null;
		PreparedStatement st = null;

		try {
			conn = DbUtil.getConnection();
			st = conn.prepareStatement("insert into WB_LOG values(?,?,?,?,?)");
			if (StringUtil.isEmpty(ip))
				ip = "-";
			if (StringUtil.isEmpty(userName))
				userName = "-";
			if (msg == null)
				msg = "-";
			st.setTimestamp(1, new Timestamp((new Date()).getTime()));
			st.setString(2, userName);
			st.setString(3, ip);
			st.setInt(4, type);
			i = Math.min(msg.length(), 256);
			while (msg.getBytes().length > 255) {
				i--;
				msg = msg.substring(0, i);
			}
			st.setString(5, msg);
			st.executeUpdate();
		} catch (Throwable e) {
		} finally {
			DbUtil.closeStatement(st);
			DbUtil.closeConnection(conn);
		}
	}

	public static void log(HttpServletRequest request, int type, String msg) {
		log((String) request.getAttribute("sys.user"), request.getRemoteAddr(),
				type, msg);
	}

	public static void message(HttpServletRequest request, String s) {
		log(request, 1, s);
	}

	public static void message(String s) {
		log(null, null, 1, s);
	}

	public static void warning(HttpServletRequest request, String s) {
		log(request, 2, s);
	}

	public static void warning(String s) {
		log(null, null, 2, s);
	}

	public static void error(HttpServletRequest request, String s) {
		log(request, 3, s);
	}

	public static void error(String s) {
		log(null, null, 3, s);
	}

	public static void message(JobExecutionContext context, long startTime) {
		message(StringUtil.concat(context.getJobDetail().getDescription(),
				" (",
				DateUtil.getHours(System.currentTimeMillis() - startTime), ")"));
	}

	public static void message(JobExecutionContext context, long startTime,
			String msg) {
		message(StringUtil.concat(context.getJobDetail().getDescription(),
				": ", msg, " (", DateUtil.getHours(System.currentTimeMillis()
						- startTime), ")"));
	}

	public static void warning(JobExecutionContext context, long startTime,
			String msg) {
		warning(StringUtil.concat(context.getJobDetail().getDescription(),
				": ", msg, " (", DateUtil.getHours(System.currentTimeMillis()
						- startTime), ")"));
	}

	public static void error(JobExecutionContext context, long startTime,
			Throwable e) {
		error(StringUtil.concat(context.getJobDetail().getDescription(), ": ",
				SysUtil.getShortError(e), " (", DateUtil.getHours(System
						.currentTimeMillis()
						- startTime), ")"));
	}
}
