package com.webbuilder.controls;

import org.json.JSONArray;

import com.webbuilder.common.Str;
import com.webbuilder.utils.JsonUtil;
import com.webbuilder.utils.StringUtil;

public class Excepter extends BackControl {
	public void create() throws Exception {
		String name = gs("name"), value = gs("value"), cmp = gs("comparator"), msg = gs("message"), result;
		boolean canExcept;

		if (StringUtil.isEmpty(name))
			name = gs("id");
		result = gp(name);
		if (StringUtil.isEmpty(cmp) || StringUtil.isSame(cmp, "notExists"))
			canExcept = StringUtil.isEmpty(result);
		else if (StringUtil.isSame(cmp, "exists"))
			canExcept = !StringUtil.isEmpty(result);
		else if (StringUtil.isSame(cmp, "="))
			canExcept = StringUtil.isEqual(result, value);
		else if (StringUtil.isSame(cmp, "<>"))
			canExcept = !StringUtil.isEqual(result, value);
		else {
			if (StringUtil.isEmpty(result))
				result = "0";
			double dval = Double.parseDouble(value);
			if (StringUtil.isSame(cmp, ">"))
				canExcept = Double.parseDouble(result) > dval;
			else if (StringUtil.isSame(cmp, "<"))
				canExcept = Double.parseDouble(result) < dval;
			else if (StringUtil.isSame(cmp, ">="))
				canExcept = Double.parseDouble(result) >= dval;
			else if (StringUtil.isSame(cmp, "<="))
				canExcept = Double.parseDouble(result) <= dval;
			else
				canExcept = false;
		}
		if (canExcept)
			if (StringUtil.isEmpty(msg))
				throw new Exception(Str.format(request, "invalidValue", name));
			else {
				if (msg.startsWith("[")) {
					JSONArray ja = new JSONArray(msg);
					int i, j = ja.length();
					String[] params = new String[j - 1];
					for (i = 1; i < j; i++)
						params[i - 1] = JsonUtil.optString(ja, i);
					throw new Exception(Str.format(request, JsonUtil.optString(
							ja, 0), params));
				} else
					throw new Exception(msg);
			}
	}
}
