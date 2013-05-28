package com.webbuilder.controls;

public class Radio extends ExtControl {
	private String key[] = { "name" };

	protected String getTagProperties() throws Exception {
		String name = gs("name");
		boolean isRadio = getMeta("xwlXtype").equals("radio");

		if (isRadio) {
			addText("inputValue", gs("id"));
			if (name.isEmpty())
				name = "rd__" + parentControl.optString("id");
		}
		addText("name", name);
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
