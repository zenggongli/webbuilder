package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;

public class Window extends ExtPanel {
	private String key[] = { "modal", "resizable", "dialog", "autoReset",
			"afterrender", "acceptEnter", "clickOK", "hide", "defaultFocus",
			"show" };
	private boolean dialog;

	public void create() throws Exception {
		setRender = false;
		createScript();
	}

	protected String getTagProperties() throws Exception {
		String modal = gs("modal"), resz = gs("resizable"), ok = gs("clickOK");
		StringBuilder exp = new StringBuilder();
		dialog = gb("dialog", true);

		super.getTagProperties();
		if (dialog) {
			exp.append("function(win){\nif(!Wb.verify(win))return;");
			if (!StringUtil.isEmpty(ok)) {
				exp.append("\n");
				exp.append(ok);
			}
			exp.append("\n}");
			addExpress("okHandler", exp.toString());
			if (StringUtil.isEmpty(modal))
				modal = "true";
			if (StringUtil.isEmpty(resz))
				resz = "false";
			addExpress("buttons", "Wb.winBtns()");
		}
		addExpress("modal", modal);
		addExpress("resizable", resz);
		return getPBuffer();
	}

	protected String getTagEvents() throws Exception {
		boolean enter = gb("acceptEnter", dialog), autoReset = gb("autoReset",
				true);
		String rnd = gs("afterrender"), hide = gs("hide"), show = gs("show"), focus = gs("defaultFocus");

		if (enter) {
			if (rnd.isEmpty())
				rnd = "Wb.monEnter(this);";
			else
				rnd = "Wb.monEnter(this);\n" + rnd;
		}
		if (!StringUtil.isEmpty(rnd))
			addEvent("afterrender", StringUtil.concat(
					"function(win,options){\n", rnd, "\n}"));
		if (autoReset) {
			if (hide.isEmpty())
				hide = "Wb.reset(win);";
			else
				hide = "Wb.reset(win);\n" + hide;
		}
		if (!hide.isEmpty())
			addEvent("hide", StringUtil.concat("function(win,options){\n",
					hide, "\n}"));
		if (!focus.isEmpty()) {
			if (show.isEmpty())
				show = focus + ".focus(false,true);";
			else
				show = StringUtil.concat(show, "\n", focus,
						".focus(false,true);");
		}
		if (!show.isEmpty())
			addEvent("show", StringUtil.concat("function(win,options){\n",
					show, "\n}"));
		return getEBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return StringUtil.merge(super.getReservedKeys(), key);
	}
}
