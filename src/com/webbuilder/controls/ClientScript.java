package com.webbuilder.controls;

public class ClientScript extends FrontControl {
	public void create() throws Exception {
		header(gs("header"));
		footer(gs("footer"));
		headerScript(gs("headerScript"));
		footerScript(gs("footerScript"));
		initScript(gs("initialize"));
		finalScript(gs("finalize"));
	}
}
