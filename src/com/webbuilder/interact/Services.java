package com.webbuilder.interact;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webbuilder.common.Str;
import com.webbuilder.tool.CustomResponse;
import com.webbuilder.tool.ExcelObject;
import com.webbuilder.tool.PrintObject;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class Services {
	public static void getIpAddress(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		request.setAttribute("ip", request.getRemoteAddr());
	}

	public static void filePush(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String data = WebUtil.fetch(request, "data");
		String file = WebUtil.fetch(request, "file");

		if (StringUtil.isEmpty(data))
			data = "";
		if (StringUtil.isEmpty(file))
			file = "file";
		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;"
				+ WebUtil.encodeFilename(request, file));
		WebUtil.response(response, data);
	}

	public static void getDateString(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = dateFormat.format(new Date());
		request.setAttribute("dateString", dateString);
	}

	public static void getProgress(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String id = request.getParameter("progressId");
		HttpSession session = request.getSession(true);
		Long p = (Long) session.getAttribute("sys.upread." + id);
		Long l = (Long) session.getAttribute("sys.uplen." + id);
		double r;

		if (p == null || l == null || l == 0)
			r = 0;
		else
			r = (double) p / l;
		WebUtil.response(response, "{value:" + Double.toString(r) + "}");
	}

	private static String getResponseString(CustomResponse resp)
			throws Exception {
		byte data[] = resp.getBytes();
		String result;

		if (data.length > 2 && data[0] == (byte) 0x1F && data[1] == (byte) 0x8B) {
			InputStream is = new GZIPInputStream(new ByteArrayInputStream(data));
			try {
				result = StringUtil.getUtfString(is);
			} finally {
				is.close();
			}
		} else
			result = new String(data, "utf-8");
		return result;
	}

	public static void preview(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String data, result, meta = request.getParameter("xwl_meta");
		if (StringUtil.isEmpty(meta) || meta.equals("[]"))
			throw new Exception(Str.format(request, "contentEmpty"));
		CustomResponse resp = new CustomResponse(response);

		request.getRequestDispatcher(request.getParameter("xwl_url")).include(
				request, resp);
		data = getResponseString(resp);
		if (!data.startsWith("{")) {
			WebUtil.response(response, data);
			return;
		}
		result = PrintObject.preview(new JSONObject(data).getJSONArray("rows"),
				new JSONArray(meta), request.getParameter("xwl_title"), request
						.getParameter("xwl_dateformat"), request
						.getParameter("xwl_timeformat"), Str.format(request,
						"preview"), request.getParameter("xwl_numText"),
				request.getParameter("xwl_numWidth"), request
						.getParameter("xwl_thousandSeparator"), request
						.getParameter("xwl_decimalSeparator"));
		WebUtil.response(response, result);
	}

	public static void download(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String data, file = request.getParameter("xwl_file"), group = request
				.getParameter("xwl_group"), meta = request
				.getParameter("xwl_meta");
		if (StringUtil.isEmpty(meta) || meta.equals("[]"))
			throw new Exception(Str.format(request, "contentEmpty"));
		byte result[];
		CustomResponse resp = new CustomResponse(response);

		request.getRequestDispatcher(request.getParameter("xwl_url")).include(
				request, resp);
		data = getResponseString(resp);
		if (data.startsWith("{success:false")) {
			WebUtil.response(response, data);
			return;
		}
		result = ExcelObject.getExcelBytes(new JSONObject(data)
				.getJSONArray("rows"), new JSONArray(meta), group, request
				.getParameter("xwl_title"), request.getParameter("xwl_sheet"),
				request.getParameter("xwl_dateformat"), request
						.getParameter("xwl_timeformat"), Str.format(request,
						"total"),
				request.getParameter("xwl_thousandSeparator"), request
						.getParameter("xwl_decimalSeparator"));
		response.setHeader("content-type", "application/force-download");
		if (StringUtil.isEmpty(file))
			file = "data";
		file = file + ".xls";
		response.setHeader("content-disposition", "attachment;"
				+ WebUtil.encodeFilename(request, file));
		WebUtil.response(response, result);
	}
}
