package com.webbuilder.controls;

public class Dispatcher extends BackControl {
	public void create() throws Exception {
		if (gs("type").equals("include"))
			request.getRequestDispatcher(gs("url")).include(request, response);
		else
			request.getRequestDispatcher(gs("url")).forward(request, response);
	}
}
