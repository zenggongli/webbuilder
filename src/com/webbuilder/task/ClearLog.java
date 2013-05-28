package com.webbuilder.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.webbuilder.common.Var;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.LogUtil;

public class ClearLog implements Job {
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		long start = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement st = null;

		try {
			conn = DbUtil.getConnection();
			st = conn.prepareStatement("delete from WB_LOG where LOG_DATE < ?");
			st.setTimestamp(1, new Timestamp(DateUtil.incDay(new Date(),
					-Var.getInt("webbuilder.task.logDays")).getTime()));
			st.executeUpdate();
			LogUtil.message(context, start);
		} catch (Throwable e) {
			LogUtil.error(context, start, e);
		} finally {
			DbUtil.closeStatement(st);
			DbUtil.closeConnection(conn);
		}
	}
}