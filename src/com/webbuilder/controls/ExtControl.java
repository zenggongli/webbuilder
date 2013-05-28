package com.webbuilder.controls;

import java.util.Iterator;

import com.webbuilder.utils.StringUtil;
import com.webbuilder.utils.WebUtil;

public class ExtControl extends FrontControl {
	private StringBuilder propertiesBuffer = new StringBuilder();
	private StringBuilder eventsBuffer = new StringBuilder();
	private StringBuilder stylesBuffer = new StringBuilder();
	private StringBuilder objectsBuffer = new StringBuilder();
	protected boolean listenersPrefix = true;
	protected boolean setRender = true;

	public void create() throws Exception {
		createScript();
	}

	protected void createScript() throws Exception {
		if (hasParent) {
			String xtype = getMeta("xwlXtype");

			if (StringUtil.isEmpty(xtype))
				xtype = "";
			else
				xtype = StringUtil.concat("xtype:\"", xtype, "\"");
			headerScript(StringUtil.concat(getComma(), "{", getJson(xtype)));
			footerScript("}");
			if (hasChild) {
				headerScriptNL(",items:[");
				footerScript("]");
			}
		} else {
			String id = gs("id");
			headerScript(StringUtil.concat("Wd.", id, "=new ",
					getMeta("xwlType"), "({"));
			footerScript("});");
			if (setRender)
				headerScript(getJson("renderTo:Ext.getBody()"));
			else
				headerScript(getJson(null));
			if (hasChild) {
				headerScriptNL(",items:[");
				footerScript("]");
			}
		}
	}

	protected String getJson(String moreProperties) throws Exception {
		StringBuilder buf = new StringBuilder(), event = new StringBuilder();
		Iterator<?> names = xwlObject.keys();
		String n, val;
		boolean addComma = false;
		String meta, tagProperties = null, tagEvents = null, metaType;
		String reserved[] = getReservedKeys(), tp, te;
		String expTypes[] = { "boolean", "bind", "express", "object", "date" };

		while (names.hasNext()) {
			n = (String) names.next();
			if (n.equals("xwlMeta") || n.equals("children") || reserved != null
					&& StringUtil.indexOf(reserved, n) != -1)
				continue;
			val = gs(n);
			if (StringUtil.isEmpty(val))
				continue;
			if (n.equals("tagProperties")) {
				tagProperties = val;
				continue;
			}
			if (n.equals("tagEvents")) {
				tagEvents = val;
				continue;
			}
			meta = getMeta(n);
			if (StringUtil.isEmpty(meta))
				meta = "string";
			metaType = StringUtil.getNamePart(meta);
			if (StringUtil.isSame(metaType, "js")) {
				if (isProperty(n)) {
					if (addComma)
						buf.append(',');
					else
						addComma = true;
					buf.append(n
							+ ":function("
							+ StringUtil.replace(StringUtil.getValuePart(meta),
									" ", "") + "){\n");
					buf.append(val);
					buf.append("\n}");
				} else {
					if (event.length() > 0)
						event.append(',');
					event.append(n
							+ ":function("
							+ StringUtil.replace(StringUtil.getValuePart(meta),
									" ", "") + "){\n");
					event.append(val);
					event.append("\n}");
				}
			} else {
				if (addComma)
					buf.append(',');
				else
					addComma = true;
				buf.append(n);
				buf.append(':');
				if (val.startsWith("@"))
					buf.append(val.substring(1));
				else if (StringUtil.indexOf(expTypes, metaType) != -1)
					buf.append(val);
				else {
					if (StringUtil.isEqual(metaType, "url"))
						val = WebUtil.getUrl(val, false);
					buf.append(StringUtil.quote(val));
				}
			}
		}
		if (!StringUtil.isEmpty(moreProperties)) {
			if (buf.length() > 0)
				buf.append(',');
			buf.append(moreProperties);
		}
		tp = getTagProperties();
		if (!StringUtil.isEmpty(tp)) {
			if (buf.length() > 0)
				buf.append(',');
			buf.append(tp);
		}
		if (!StringUtil.isEmpty(tagProperties)) {
			if (buf.length() > 0)
				buf.append(',');
			buf.append(tagProperties);
		}
		te = getTagEvents();
		if (!StringUtil.isEmpty(te)) {
			if (event.length() > 0)
				event.append(',');
			event.append(te);
		}
		if (!StringUtil.isEmpty(tagEvents)) {
			if (event.length() > 0)
				event.append(',');
			event.append(tagEvents);
		}
		if (event.length() > 0) {
			if (listenersPrefix) {
				buf.append(",listeners:{");
				buf.append(event.toString());
				buf.append('}');
			} else {
				buf.append(',');
				buf.append(event.toString());
			}
		}
		return buf.toString();
	}

	protected String[] getReservedKeys() throws Exception {
		return null;
	}

	protected String getTagProperties() throws Exception {
		return "";
	}

	protected String getTagEvents() throws Exception {
		return "";
	}

	protected void addText(String key, String value) {
		if (StringUtil.isEmpty(value))
			return;
		if (propertiesBuffer.length() > 0)
			propertiesBuffer.append(',');
		propertiesBuffer.append(key);
		propertiesBuffer.append(':');
		if (value.startsWith("@"))
			propertiesBuffer.append(value.substring(1));
		else
			propertiesBuffer.append(StringUtil.quote(value));
	}

	protected void addExpress(String key, String value) {
		if (StringUtil.isEmpty(value))
			return;
		if (propertiesBuffer.length() > 0)
			propertiesBuffer.append(',');
		propertiesBuffer.append(key);
		propertiesBuffer.append(':');
		if (value.startsWith("@"))
			propertiesBuffer.append(value.substring(1));
		else
			propertiesBuffer.append(value);
	}

	protected void addEvent(String name, String script) {
		if (StringUtil.isEmpty(script))
			return;
		if (eventsBuffer.length() > 0)
			eventsBuffer.append(',');
		eventsBuffer.append(name);
		eventsBuffer.append(':');
		eventsBuffer.append(script);
	}

	protected void setStyles(String styles) {
		if (StringUtil.isEmpty(styles))
			return;
		if (stylesBuffer.length() > 0)
			stylesBuffer.append(';');
		stylesBuffer.append(styles);
	}

	protected void addStyle(String key, String value) {
		if (StringUtil.isEmpty(value))
			return;
		if (stylesBuffer.length() > 0)
			stylesBuffer.append(';');
		stylesBuffer.append(key);
		stylesBuffer.append(':');
		stylesBuffer.append(value);
	}

	protected void setObjects(String objects) {
		if (objects != null && objects.startsWith("{"))
			objects = objects.substring(1, objects.length() - 1);
		if (StringUtil.isEmpty(objects))
			return;
		if (objectsBuffer.length() > 0)
			objectsBuffer.append(',');
		objectsBuffer.append(objects);
	}

	protected void addObject(String key, String value) {
		if (StringUtil.isEmpty(value))
			return;
		if (objectsBuffer.length() > 0)
			objectsBuffer.append(',');
		objectsBuffer.append(key);
		objectsBuffer.append(':');
		if (value.startsWith("@"))
			objectsBuffer.append(value.substring(1));
		else
			objectsBuffer.append(value);
	}

	protected String getPBuffer() {
		return propertiesBuffer.toString();
	}

	protected String getEBuffer() {
		return eventsBuffer.toString();
	}

	protected String getOBuffer() {
		String s = objectsBuffer.toString();
		if (StringUtil.isEmpty(s))
			return "";
		else
			return StringUtil.concat("{", objectsBuffer.toString(), "}");
	}

	protected String getSBuffer() {
		return stylesBuffer.toString();
	}

	protected void resetSBuffer() {
		stylesBuffer.delete(0, stylesBuffer.length());
	}

	protected void resetPBuffer() {
		propertiesBuffer.delete(0, propertiesBuffer.length());
	}

	protected void resetEBuffer() {
		eventsBuffer.delete(0, eventsBuffer.length());
	}
}
