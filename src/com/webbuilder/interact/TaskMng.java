package com.webbuilder.interact;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import com.webbuilder.common.TaskService;
import com.webbuilder.common.Var;
import com.webbuilder.tool.PageInfo;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.DbUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class TaskMng {
	public static void getTaskList(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		request.setAttribute("findTask", StringUtil.replaceParameters(request,
				"%{#findTask#}%"));
		Scheduler sched = TaskService.getScheduler();
		ResultSet rs = DbUtil.query(request,
				"select * from WB_TASK where TASK_NAME like {?findTask?}");
		Date date;
		Trigger[] triggers;
		StringBuilder buf = new StringBuilder();
		String jobId;
		int cp;
		boolean first = true, hasTrigger;
		PageInfo pageInfo = WebUtil.getPage(request);

		buf.append(",rows:[");
		while (rs.next()) {
			cp = WebUtil.checkPage(pageInfo);
			if (cp == 1)
				break;
			else if (cp == 2)
				continue;
			if (first)
				first = false;
			else
				buf.append(',');
			jobId = rs.getString("TASK_ID");
			buf.append("{taskId:");
			buf.append(StringUtil.quote(jobId));
			buf.append(",taskName:");
			buf.append(StringUtil.quote(rs.getString("TASK_NAME")));
			buf.append(",intervalType:");
			buf.append(rs.getInt("INTERVAL_TYPE"));
			buf.append(",express:");
			buf.append(StringUtil.quote(rs.getString("INTERVAL_EXPRESS")));
			buf.append(",clsName:");
			buf.append(StringUtil.quote(rs.getString("CLASS_NAME")));
			buf.append(",beginDate:\"");
			date = rs.getTimestamp("BEGIN_DATE");
			if (date == null)
				buf.append("");
			else
				buf.append(DateUtil.toString(date));
			buf.append("\",endDate:\"");
			date = rs.getTimestamp("END_DATE");
			if (date == null)
				buf.append("");
			else
				buf.append(DateUtil.toString(date));
			buf.append("\",taskStatus:");
			buf.append(rs.getInt("STATUS"));
			triggers = sched.getTriggersOfJob(jobId, Scheduler.DEFAULT_GROUP);
			buf.append(",previous:\"");
			hasTrigger = triggers != null && triggers.length > 0;
			if (hasTrigger)
				buf
						.append(DateUtil.toString(triggers[0]
								.getPreviousFireTime()));
			else
				buf.append("");
			buf.append("\",next:\"");
			if (hasTrigger)
				buf.append(DateUtil.toString(triggers[0].getNextFireTime()));
			else
				buf.append("");
			buf.append("\",remark:");
			buf.append(StringUtil.quote(rs.getString("REMARK")));
			buf.append('}');
		}
		buf.append("]}");
		WebUtil.setTotal(buf, pageInfo);
		WebUtil.response(response, buf);
	}

	public static void startTasks(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		TaskService.shutdown();
		loadTasks(null, request);
		Var.set("server.startTask", true);
	}

	public static void shutdownTasks(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		TaskService.shutdown();
		Var.set("server.startTask", false);
	}

	public static void pauseResumeTasks(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Scheduler scheduler = TaskService.getScheduler();
		JSONArray rows = new JSONArray(request.getParameter("grid1"));
		String id;
		ArrayList<String> ids;
		int i, j = rows.length();
		boolean isFirst = true, isPause = !StringUtil.getBool(request
				.getParameter("status"));
		StringBuilder buf = new StringBuilder();
		Trigger trigger = null, triggers[];

		if (isPause)
			ids = null;
		else
			ids = new ArrayList<String>();
		for (i = 0; i < j; i++) {
			id = rows.getJSONObject(i).getString("taskId");
			if (isPause)
				scheduler.deleteJob(id, Scheduler.DEFAULT_GROUP);
			else
				ids.add(id);
		}
		if (isPause)
			WebUtil.response(response, "[]");
		else {
			loadTasks(ids.toArray(new String[ids.size()]), request);
			Thread.sleep(20);// Wait for getNextFireTime
			buf.append('[');
			for (String s : ids) {
				triggers = scheduler.getTriggersOfJob(s,
						Scheduler.DEFAULT_GROUP);
				if (triggers == null || triggers.length == 0)
					trigger = null;
				else
					trigger = triggers[0];
				if (isFirst)
					isFirst = false;
				else
					buf.append(',');
				buf.append("{p:\"");
				if (trigger != null)
					buf
							.append(DateUtil.toString(trigger
									.getPreviousFireTime()));
				buf.append("\",n:\"");
				if (trigger != null)
					buf.append(DateUtil.toString(trigger.getNextFireTime()));
				buf.append("\"}");

			}
			buf.append(']');
			WebUtil.response(response, buf);
		}
	}

	public static void removeTasks(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Scheduler scheduler = TaskService.getScheduler();
		JSONArray rows = new JSONArray(request.getParameter("grid1"));
		int i, j = rows.length();

		for (i = 0; i < j; i++) {
			scheduler.deleteJob(rows.getJSONObject(i).getString("taskId"),
					Scheduler.DEFAULT_GROUP);
		}
	}

	private static void runTask(HttpServletRequest request,
			HttpServletResponse response, String id, boolean dateOnly)
			throws Exception {
		loadTasks(StringUtil.getList(id), request);
		Thread.sleep(20);// Wait for getNextFireTime
		Trigger tgs[] = TaskService.getScheduler().getTriggersOfJob(id,
				Scheduler.DEFAULT_GROUP);
		JSONObject jo = new JSONObject();

		if (!dateOnly) {
			jo.put("taskId", id);
			jo.put("taskStatus", 1);
		}
		if (tgs != null && tgs.length > 0) {
			jo.put("previous", DateUtil.toString(tgs[0].getPreviousFireTime()));
			jo.put("next", DateUtil.toString(tgs[0].getNextFireTime()));
		} else {
			jo.put("previous", (String) null);
			jo.put("next", (String) null);
		}
		WebUtil.response(response, jo);
	}

	public static void startTask(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		runTask(request, response, (String) request.getAttribute("sys.id"),
				false);
	}

	public static void restartTask(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id = request.getParameter("taskId");
		TaskService.getScheduler().deleteJob(id, Scheduler.DEFAULT_GROUP);
		runTask(request, response, id, true);
	}

	public static void startup() throws Exception {
		loadTasks(null, null);
	}

	private static void loadTasks(String[] ids, HttpServletRequest request)
			throws Exception {
		Connection conn = null;
		ResultSet rs = null;
		Scheduler sched = TaskService.getScheduler();
		JobDetail job;
		Date date;
		Trigger trigger = null;
		String id, express[], e = "Invalid interval expression.";

		try {
			if (request == null) {
				conn = DbUtil.getConnection();
				rs = DbUtil.getResultSet(conn, "select * from WB_TASK");
			} else
				rs = DbUtil.query(request, "select * from WB_TASK");
			while (rs.next()) {
				id = rs.getString("TASK_ID");
				if (ids != null && StringUtil.indexOf(ids, id) == -1)
					continue;
				if (rs.getInt("STATUS") == 1
						&& sched.getJobDetail(id, Scheduler.DEFAULT_GROUP) == null) {
					job = new JobDetail(id, Scheduler.DEFAULT_GROUP, Class
							.forName(rs.getString("CLASS_NAME")));
					job.setDescription(rs.getString("TASK_NAME"));
					express = StringUtil.split(
							rs.getString("INTERVAL_EXPRESS"), ":");
					switch (rs.getInt("INTERVAL_TYPE")) {
					case 0:
						trigger = TriggerUtils.makeSecondlyTrigger(Integer
								.parseInt(express[0]));
						break;
					case 1:
						trigger = TriggerUtils.makeMinutelyTrigger(Integer
								.parseInt(express[0]));
						break;
					case 2:
						trigger = TriggerUtils.makeHourlyTrigger(Integer
								.parseInt(express[0]));
						break;
					case 3:
						if (express.length != 2)
							throw new Exception(e);
						trigger = TriggerUtils.makeDailyTrigger(Integer
								.parseInt(express[0]), Integer
								.parseInt(express[1]));
						break;
					case 4:
						if (express.length != 3)
							throw new Exception(e);
						trigger = TriggerUtils.makeWeeklyTrigger(Integer
								.parseInt(express[0]), Integer
								.parseInt(express[1]), Integer
								.parseInt(express[2]));
						break;
					case 5:
						if (express.length != 3)
							throw new Exception(e);
						trigger = TriggerUtils.makeMonthlyTrigger(Integer
								.parseInt(express[0]), Integer
								.parseInt(express[1]), Integer
								.parseInt(express[2]));
						break;
					}
					trigger.setName(id);
					date = rs.getTimestamp("BEGIN_DATE");
					if (date != null)
						trigger.setStartTime(date);
					date = rs.getTimestamp("END_DATE");
					if (date != null)
						trigger.setEndTime(date);
					sched.scheduleJob(job, trigger);
				}
			}
			sched.start();
		} finally {
			if (request == null) {
				DbUtil.closeResultSet(rs);
				DbUtil.closeConnection(conn);
			}
		}
	}
}
