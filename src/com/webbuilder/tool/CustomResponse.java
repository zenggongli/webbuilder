package com.webbuilder.tool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class CustomResponse extends HttpServletResponseWrapper {
	public int respCode;
	public String respMsg;
	private PrintWriter writer;
	private ServletOutputStream sos;
	private ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
	private boolean submited = false;

	public CustomResponse(HttpServletResponse response) {
		super(response);
	}

	public byte[] getBytes() {
		return bos.toByteArray();
	}

	public String getString() throws UnsupportedEncodingException {
		return new String(getBytes(), "utf-8");
	}

	public void sendError(int sc) throws IOException {
		respCode = sc;
	}

	public void sendError(int sc, String msg) throws IOException {
		respCode = sc;
		respMsg = msg;
	}

	public void setStatus(int sc) {
		respCode = sc;
	}

	public void setStatus(int sc, String sm) {
		respCode = sc;
		respMsg = sm;
	}

	public void flushBuffer() throws IOException {
		submited = true;
	}

	public int getBufferSize() {
		return 8192;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (sos != null)
			return sos;
		sos = new ServletOutputStream() {
			public void write(byte[] data, int offset, int length) {
				if (!submited)
					bos.write(data, offset, length);
			}

			public void write(int b) throws IOException {
				if (!submited)
					bos.write(b);
			}
		};
		return sos;
	}

	public PrintWriter getWriter() throws IOException {
		if (writer != null)
			return writer;
		writer = new PrintWriter(getOutputStream());
		return writer;
	}

	public boolean isCommitted() {
		return submited;
	}

	public void reset() {
		if (!submited)
			bos.reset();
	}

	public void resetBuffer() {
		reset();
	}
}