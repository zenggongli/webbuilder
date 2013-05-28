package com.webbuilder.interact;

import java.io.File;
import java.io.FileInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webbuilder.common.Main;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.SysUtil;
import com.webbuilder.utils.WebUtil;

public class FileSystem {
	public static void getFileTree(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String dir = request.getParameter("dir");
		boolean check = StringUtil.getBool(request.getParameter("check"));
		File[] files;

		if (StringUtil.isEmpty(dir))
			files = File.listRoots();
		else if (dir.equals("@"))
			files = Main.path.listFiles();
		else
			files = new File(dir).listFiles();
		FileUtil.sortFiles(files);
		WebUtil.response(response, getFilesInfo(files, check));
	}

	private static String getFilesInfo(File[] files, boolean check) {
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true, isDir;
		String name, dir;

		buf.append('[');
		for (File file : files) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(',');
			name = file.getName();
			dir = FileUtil.getPath(file);
			if (StringUtil.isEmpty(name))
				name = FileUtil.extractDir(dir);
			buf.append("{text:\"");
			buf.append(name);
			buf.append("\",dir:\"");
			buf.append(dir);
			buf.append("\"");
			isDir = file.isDirectory();
			buf.append(",isDir:");
			buf.append(isDir);
			if (!FileUtil.hasSubFile(file, false)) {
				if (isDir)
					buf.append(",children:[]");
				else {
					if (check)
						buf.append(",checked:false");
					buf
							.append(",leaf:true,iconCls:\"default_icon\",icon:\"main?xwl=13MY44A9AOKN&file=\"+encodeURIComponent(\""
									+ dir + "\")");
				}
			}
			buf.append('}');
		}
		buf.append(']');
		return buf.toString();
	}

	public static void getIcon(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String fileName = WebUtil.decode(request.getParameter("file"));
		String fileExt = FileUtil.extractFileExt(fileName).toLowerCase();
		String imgTypes[] = { "gif", "jpg", "png", "bmp" };
		String zipTypes[] = { "zip", "rar", "gzip", "gz", "tar", "cab" };
		File file = null;

		response.reset();
		if (StringUtil.indexOf(imgTypes, fileExt) != -1) {
			file = new File(fileName);
			long fileLen = file.length();
			if (fileLen > 10240 || fileLen == 0) {
				fileName = "image";
				file = null;
			}
		} else if (StringUtil.indexOf(zipTypes, fileExt) != -1)
			fileName = "zip";
		else if (fileExt.equals("txt"))
			fileName = "text";
		else if (fileExt.equals("doc"))
			fileName = "word";
		else if (fileExt.equals("xls"))
			fileName = "excel";
		else if (fileExt.equals("ppt"))
			fileName = "ppt";
		else if (fileExt.equals("htm") || fileExt.equals("html"))
			fileName = "web";
		else
			fileName = "file";
		if (file == null) {
			file = new File(Main.path, StringUtil.concat("webbuilder/images/",
					fileName + ".gif"));
			response.setContentType("image/gif");
		} else
			response.setContentType("image/" + fileExt);
		FileInputStream is = new FileInputStream(file);
		try {
			SysUtil.isToOs(is, response.getOutputStream());
		} finally {
			is.close();
		}
		response.flushBuffer();
	}
}
