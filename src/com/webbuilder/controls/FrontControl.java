package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class FrontControl extends Control {
	private StringBuilder sbHeader = new StringBuilder();
	private StringBuilder sbFooter = new StringBuilder();
	private StringBuilder sbHeaderScript = new StringBuilder();
	private StringBuilder sbFooterScript = new StringBuilder();
	private StringBuilder sbInitScript = new StringBuilder();
	private StringBuilder sbFinalScript = new StringBuilder();

	public void create() throws Exception {
	}

	protected void header(String t) {
		if (!StringUtil.isEmpty(t)) {
			if (sbHeader.length() > 0)
				sbHeader.append("\n");
			sbHeader.append(t);
		}
	}

	protected void footer(String t) {
		if (!StringUtil.isEmpty(t)) {
			if (sbFooter.length() > 0)
				sbFooter.insert(0, "\n");
			sbFooter.insert(0, t);
		}
	}

	protected String getComma() {
		if (isFirstChild)
			return "";
		else
			return ",";
	}

	protected void headerScript(String t) {
		if (!StringUtil.isEmpty(t)) {
			if (sbHeaderScript.length() > 0)
				sbHeaderScript.append("\n");
			sbHeaderScript.append(t);
		}
	}

	protected void headerScriptNL(String t) {
		if (!StringUtil.isEmpty(t)) {
			sbHeaderScript.append(t);
		}
	}

	protected void footerScript(String t) {
		if (!StringUtil.isEmpty(t)) {
			sbFooterScript.insert(0, t);
		}
	}

	protected void initScript(String t) {
		if (!StringUtil.isEmpty(t)) {
			if (sbInitScript.length() > 0)
				sbInitScript.append("\n");
			sbInitScript.append(t);
		}
	}

	protected void finalScript(String t) {
		if (!StringUtil.isEmpty(t)) {
			if (sbFinalScript.length() > 0)
				sbFinalScript.insert(0, "\n");
			sbFinalScript.insert(0, t);
		}
	}

	public String getHeader() {
		return sbHeader.toString();
	}

	public String getFooter() {
		return sbFooter.toString();
	}

	public String getHeaderScript() {
		return sbHeaderScript.toString();
	}

	public String getFooterScript() {
		return sbFooterScript.toString();
	}

	public String getInitScript() {
		return sbInitScript.toString();
	}

	public String getFinalScript() {
		return sbFinalScript.toString();
	}

	protected String ge(String name) throws Exception {
		String s = gs(name);
		if (s.startsWith("@"))
			return s.substring(1);
		else
			return StringUtil.quote(s);
	}
}
