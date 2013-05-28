package com.webbuilder.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.syspatch.zip.ZipEntry;
import com.syspatch.zip.ZipInputStream;
import com.syspatch.zip.ZipOutputStream;
import com.webbuilder.common.Var;

public class ZipUtil {
	public static void zip(File source[], OutputStream outputStream)
			throws Exception {
		ZipOutputStream zipStream = new ZipOutputStream(outputStream);
		zipStream.fileCharset = Var.get("server.fileNameCharset");
		try {
			for (File file : source)
				zip(file, zipStream, file.getName());
		} finally {
			zipStream.close();
		}
	}

	public static void zip(File source[], File zipFile) throws Exception {
		zip(source, new FileOutputStream(zipFile));
	}

	private static void zip(File source, ZipOutputStream zipStream, String base)
			throws Exception {
		ZipEntry entry;

		if (source.isDirectory()) {
			entry = new ZipEntry(base + "/");
			entry.setTime(source.lastModified());
			zipStream.putNextEntry(entry);
			if (!StringUtil.isEmpty(base))
				base += "/";
			File[] fileList = source.listFiles();
			for (File file : fileList)
				zip(file, zipStream, base + file.getName());
		} else {
			entry = new ZipEntry(base);
			entry.setTime(source.lastModified());
			zipStream.putNextEntry(entry);
			FileInputStream in = new FileInputStream(source);
			try {
				SysUtil.isToOs(in, zipStream);
			} finally {
				in.close();
			}
		}
	}

	public static void unzip(InputStream inputStream, File dest)
			throws Exception {
		ZipInputStream zipStream = new ZipInputStream(inputStream);
		zipStream.fileCharset = Var.get("server.fileNameCharset");
		ZipEntry z;
		File f;
		String name;
		FileOutputStream out;

		try {
			while ((z = zipStream.getNextEntry()) != null) {
				name = z.getName();
				if (z.isDirectory()) {
					name = name.substring(0, name.length() - 1);
					f = new File(dest, name);
					if (!f.exists())
						f.mkdir();
				} else {
					f = new File(dest, name);
					if (!f.exists())
						f.createNewFile();
					out = new FileOutputStream(f);
					try {
						SysUtil.isToOs(zipStream, out);
					} finally {
						out.close();
					}
				}
				f.setLastModified(z.getTime());
			}
		} finally {
			zipStream.close();
		}
	}

	public static void unzip(File zipFile, File dest) throws Exception {
		unzip(new FileInputStream(zipFile), dest);
	}
}