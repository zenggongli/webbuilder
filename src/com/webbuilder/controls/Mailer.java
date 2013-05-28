package com.webbuilder.controls;

import com.webbuilder.tool.MailObject;

public class Mailer extends BackControl {
	public void create() throws Exception {
		if (gb("disabled", false))
			return;
		MailObject mail = new MailObject(gs("smtp"), gs("username"),
				gs("password"), gb("needAuth", true));
		try {
			mail.send(gs("from"), gs("to"), gs("cc"), gs("bcc"), gs("title"),
					gs("content"), gs("attachFiles"), request,
					gs("attachObjects"), gs("attachObjectNames"));
		} finally {
			mail.close();
		}
	}
}
