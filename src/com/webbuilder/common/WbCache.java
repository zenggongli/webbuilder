package com.webbuilder.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.SysUtil;

public class WbCache extends HttpServlet {
	private static final long serialVersionUID = 8209317525417017635L;
	private static final ConcurrentHashMap<String, Object[]> resCache = new ConcurrentHashMap<String, Object[]>();
	private static ServletContext servletContext;
	private static boolean useCache;
	private static boolean uncheckModified;
	private static int cacheGzipMinSize;

	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		try {
			String path = "webbuilder" + request.getPathInfo();
			Object[] obj;
			File file;
			byte[] bt = null;
			boolean isGzip = false;
			long lastModified;

			if (uncheckModified) {
				file = null;
				lastModified = -1;
			} else {
				file = new File(Main.path, path);
				lastModified = file.lastModified();
			}
			if (useCache) {
				obj = resCache.get(path);
				if (obj != null) {
					if (uncheckModified || lastModified == (Long) obj[2]) {
						isGzip = (Boolean) obj[0];
						bt = (byte[]) obj[1];
						if (uncheckModified)
							lastModified = (Long) obj[2];
					}
				}
			}
			if (bt == null) {
				if (uncheckModified) {
					file = new File(Main.path, path);
					lastModified = file.lastModified();
				}
				if (lastModified == 0) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, path);
					return;
				}
				isGzip = useCache && file.length() >= cacheGzipMinSize;
				bt = getResourceByte(file, isGzip);
				if (useCache) {
					obj = new Object[3];
					obj[0] = isGzip;
					obj[1] = bt;
					obj[2] = lastModified;
					resCache.put(path, obj);
				}
			}
			String fileEtag = Long.toString(lastModified), reqEtag = request
					.getHeader("If-None-Match");
			if (StringUtil.isEqual(reqEtag, fileEtag)) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
			response.setHeader("Etag", fileEtag);
			if (isGzip)
				response.setHeader("Content-Encoding", "gzip");
			response.setContentType(servletContext.getMimeType(path));
			response.setContentLength(bt.length);
			response.getOutputStream().write(bt);
			response.flushBuffer();
		} catch (Throwable e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public void init() throws ServletException {
		super.init();
		servletContext = getServletContext();
		try {
			useCache = Var.getBool("server.cacheEnabled");
			uncheckModified = !Var.getBool("server.cacheCheckModified");
			cacheGzipMinSize = Var.getInt("server.cacheGzipMinSize");
		} catch (Throwable e) {
			useCache = true;
			uncheckModified = true;
			cacheGzipMinSize = 5120;
		}
	}

	private byte[] getResourceByte(File file, boolean isZip) throws Exception {
		InputStream is = new FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] bt;

		try {
			if (isZip) {
				GZIPOutputStream gos = new GZIPOutputStream(bos);
				try {
					SysUtil.isToOs(is, gos);
				} finally {
					gos.close();
				}
			} else
				SysUtil.isToOs(is, bos);
			bt = bos.toByteArray();
		} finally {
			is.close();
		}
		return bt;
	}
}
