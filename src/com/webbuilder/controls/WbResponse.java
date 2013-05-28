package com.webbuilder.controls;

import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class WbResponse extends BackControl {
	public void create() throws Exception {
		String obj = gs("object"), s;

		if (obj.isEmpty())
			s = gs("text");
		else
			s = gp(obj);
		if (gb("uploadResponse", WebUtil.isFormSubmit(request)))
			s = StringUtil.concat("{success:true,value:", StringUtil
					.quote(StringUtil.convertHTML(s)), "}");
		WebUtil.response(response, s);
	}
}
