/**  
 * WebBuilder   
 * @author Chen Jie  
 * @version 6.5
 */
package com.webbuilder.common;

import java.io.File;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webbuilder.interact.Install;
import com.webbuilder.interact.MngTool;

public class Main extends HttpServlet {
	private static final long serialVersionUID = -8953575363658619052L;
	public static File path;
	public static boolean installed = true;
	public static Date startTime;

	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException {
		try {
			request.setCharacterEncoding("utf-8");
			response.setContentType("text/html;charset=utf-8");
			if (installed) {
				String xwl = request.getParameter("xwl");
				if (xwl == null)
					xwl = Var.get("webbuilder.portal");
				Parser parser = new Parser(request, response, xwl);
				parser.parse();
			} else
				Install.setup(request, response);
		} catch (Throwable e) {
			if (Var.getBool("server.printError", true))
				throw new ServletException(e);
		}
	}

	public void init() throws ServletException {
		super.init();
		startTime = new Date();
		ServletContext ctx = getServletContext();
		path = new File(ctx.getRealPath("/"));
		installed = Install.checkInstall();
		try {
			if (installed) {
				MngTool.loadSystem(true);
				Install.checkUpdate(ctx.getContextPath());
			} else
				Var.loadServerVar();
		} catch (Throwable e) {
			throw new ServletException(e);
		}
	}
}