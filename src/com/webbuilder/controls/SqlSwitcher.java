package com.webbuilder.controls;

import com.webbuilder.common.Var;

public class SqlSwitcher extends BackControl {
	public void create() throws Exception {
		String sql = gs(Var.get("server.dbType"));

		if (sql.isEmpty())
			sql = gs("default");
		while (sql.startsWith("@"))
			sql = gs(sql.substring(1));
		request.setAttribute(gs("id"), sql);
	}
}
