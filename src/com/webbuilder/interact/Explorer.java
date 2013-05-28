package com.webbuilder.interact;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.webbuilder.common.Main;
import com.webbuilder.common.Str;
import com.webbuilder.common.Var;
import com.webbuilder.utils.DateUtil;
import com.webbuilder.utils.FileUtil;
import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;
import com.webbuilder.utils.ZipUtil;

public class Explorer {
	public static void deleteFiles(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(request.getParameter("files"));
		int i, j = ja.length();
		File file;
		boolean deleted;

		for (i = 0; i < j; i++) {
			file = new File(ja.optString(i));
			if (file.isDirectory())
				deleted = FileUtil.deleteFolder(file);
			else
				deleted = file.delete();
			if (!deleted)
				throw new Exception(Str.format("cannotDelete", file.getName()));
		}
	}

	public static void pasteFiles(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(request.getParameter("files"));
		File file, newFile, destDir = new File(request.getParameter("dir"));
		boolean isCut = StringUtil.getBool(request.getParameter("isCut"));
		int i, j = ja.length();

		for (i = 0; i < j; i++) {
			file = new File(ja.getString(i));
			newFile = new File(destDir, file.getName());
			if (file.isDirectory() && FileUtil.isAncestor(file, newFile))
				throw new Exception(Str.format(request, "cannotAppend"));
			if (file.isDirectory())
				FileUtil.copyFolder(file, newFile, true, isCut);
			else
				FileUtil.copyFile(file, newFile, true, isCut);
		}
	}

	public static void exportFiles(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray(request.getParameter("files"));
		int i, j = ja.length();
		File[] files = new File[j];
		boolean useZip;
		String fileName;

		for (i = 0; i < j; i++)
			files[i] = new File(ja.optString(i));
		fileName = files[0].getName();
		useZip = StringUtil.isEqual(request.getParameter("type"), "1") || j > 1
				|| files[0].isDirectory();
		if (j == 1) {
			if (useZip)
				fileName = FileUtil.extractFilenameNoExt(fileName) + ".zip";
		} else {
			File parentFile = files[0].getParentFile();
			if (parentFile == null)
				fileName = "file.zip";
			else
				fileName = parentFile.getName() + ".zip";
		}
		if (fileName.equals(".zip") || fileName.equals("/.zip"))
			fileName = "file.zip";
		response.reset();
		if (!useZip)
			response.setHeader("content-length", Long.toString(files[0]
					.length()));
		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;"
				+ WebUtil.encodeFilename(request, fileName));
		if (useZip) {
			ZipUtil.zip(files, response.getOutputStream());
			response.flushBuffer();
		} else
			WebUtil.response(response, new FileInputStream(files[0]));
	}

	public static void importFile(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String dir = request.getAttribute("dir").toString();
		InputStream stream = (InputStream) request.getAttribute("uploadFile");
		String fileName = request.getAttribute("uploadFile__name").toString();

		if (StringUtil.isEqual(request.getAttribute("type").toString(), "1")) {
			if (StringUtil.isSame(FileUtil.extractFileExt(fileName), "zip"))
				ZipUtil.unzip(stream, new File(dir));
			else
				throw new Exception(Str.format(request, "selectZip"));
		} else
			FileUtil.saveStream(stream, new File(dir, fileName));
	}

	public static void writeFile(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		File file = new File(request.getParameter("file"));
		String cs = request.getParameter("charsetCombo");
		if (cs != null && cs.startsWith("["))
			cs = null;
		FileUtil.writeText(file, request.getParameter("editor"), cs);
		StringBuilder buf = new StringBuilder();
		loadFileInfo(file, buf);
		WebUtil.response(response, buf);
	}

	public static void readFile(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		File file = new File(request.getParameter("file"));
		String cs = request.getParameter("charsetCombo");
		if (cs != null && cs.startsWith("["))
			cs = null;
		WebUtil.response(response, FileUtil.readText(file, cs));
	}

	public static void newFile(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		boolean result;

		File file = new File(request.getParameter("dir"), request
				.getParameter("newName"));
		if (StringUtil.getBool(request.getParameter("isDir")))
			result = file.mkdir();
		else
			result = file.createNewFile();
		if (!result)
			throw new Exception(Str.format(request, "failedExecute"));
		StringBuilder buf = new StringBuilder();
		loadFileInfo(file, buf);
		WebUtil.response(response, buf);
	}

	public static void rename(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String newName = request.getParameter("newName");
		File file = new File(request.getParameter("dir"));
		File newFile = new File(file.getParent(), newName);

		if (newName.indexOf('/') != -1 || newName.indexOf('\\') != -1
				|| !file.renameTo(newFile))
			throw new Exception(Str.format(request, "failedExecute"));
		WebUtil.response(response, "{dir:"
				+ StringUtil.quote(FileUtil.getPath(newFile)) + "}");
	}

	public static void getFile(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		File dir = new File(request.getParameter("dir"));
		File[] fs = dir.listFiles();

		if (fs == null || fs.length == 0) {
			WebUtil.response(response, "{total:0,rows:[]}");
			return;
		}
		String[] sortInfo = WebUtil.getSortInfo(request), fields = { "name",
				"size", "type", "date" };
		StringBuilder buf = new StringBuilder();
		boolean isFirst = true;
		int folderCount = 0;

		FileUtil.sortFiles(fs, StringUtil.indexOf(fields, sortInfo[0]),
				sortInfo[1].equalsIgnoreCase("desc"));
		buf.append("{total:");
		buf.append(fs.length);
		for (File f : fs)
			if (f.isDirectory())
				folderCount++;
		buf.append(",folders:");
		buf.append(folderCount);
		buf.append(",rows:[");
		for (File file : fs) {
			if (isFirst)
				isFirst = false;
			else
				buf.append(",");
			loadFileInfo(file, buf);
		}
		buf.append("]}");
		WebUtil.response(response, buf);
	}

	private static void loadFileInfo(File file, StringBuilder buf) {
		boolean isDir = file.isDirectory();
		buf.append("{text:");
		buf.append(StringUtil.quote(file.getName()));
		buf.append(",size:");
		if (isDir)
			buf.append("null");
		else
			buf.append(file.length());
		buf.append(",isDir:");
		buf.append(isDir);
		buf.append(",dir:");
		buf.append(StringUtil.quote(FileUtil.getPath(file)));
		buf.append(",type:");
		if (isDir)
			buf.append("Str.folder");
		else
			buf.append(StringUtil.quote(FileUtil.getFileType(file)));
		buf.append(",date:\"");
		buf.append(DateUtil.toString(new Date(file.lastModified())));
		buf.append("\"}");
	}

	public static void init(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String path = FileUtil.getPath(Main.path);
		File fs[];
		int type = Var.getInt("webbuilder.app.file.rootBaseType");

		if (type == 4)
			path = "";
		else if (type == 3)
			path = Main.path.getName();
		else if (type == 2)
			path = Main.path.getParentFile().getName() + "/"
					+ Main.path.getName();
		else {
			fs = File.listRoots();
			if (fs == null || fs.length == 0) {
				fs = Main.path.getParentFile().listFiles();
				if (fs == null || fs.length == 0)
					path = "";
				else
					path = Main.path.getName();
			}
		}
		request.setAttribute("serverPath", path);
	}

	public static void getDir(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String dir = request.getParameter("dir");
		File fs[];

		if (StringUtil.isEmpty(dir)) {
			int type = Var.getInt("webbuilder.app.file.rootBaseType");
			if (type == 4)
				fs = Main.path.listFiles();
			else if (type == 3)
				fs = Main.path.getParentFile().listFiles();
			else if (type == 2)
				fs = Main.path.getParentFile().getParentFile().listFiles();
			else {
				fs = File.listRoots();
				if (fs == null || fs.length == 0) {
					fs = Main.path.getParentFile().listFiles();
					if (fs == null || fs.length == 0)
						fs = Main.path.listFiles();
				}
			}
		} else {
			fs = new File(dir).listFiles();
		}
		FileUtil.sortFiles(fs);
		WebUtil.response(response, getTree(fs));
	}

	private static String getTree(File[] files) {
		boolean canAccess = false, isFirst = true;
		String name, dir;
		StringBuilder buf = new StringBuilder();

		buf.append("[");
		for (File file : files) {
			if (!file.isDirectory())
				continue;
			if (!canAccess)
				canAccess = true;
			if (isFirst)
				isFirst = false;
			else
				buf.append(",");
			buf.append("{text:");
			name = file.getName();
			dir = FileUtil.getPath(file);
			if (StringUtil.isEmpty(name))
				name = FileUtil.extractDir(dir);
			buf.append(StringUtil.quote(name));
			buf.append(",dir:");
			buf.append(StringUtil.quote(dir));
			if (!FileUtil.hasSubFile(file, true))
				buf.append(",children:[]");
			buf.append("}");
		}
		buf.append("]");
		return buf.toString();
	}
}
