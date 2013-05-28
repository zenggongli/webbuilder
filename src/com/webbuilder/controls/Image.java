package com.webbuilder.controls;

public class Image extends ExtControl {
	private String key[] = { "stretch", "width", "height" };

	protected String getTagProperties() throws Exception {
		if (gb("stretch", true)) {
			addExpress("width", gs("width"));
			addExpress("height", gs("height"));
		}
		return getPBuffer();
	}

	protected String[] getReservedKeys() throws Exception {
		return key;
	}
}
