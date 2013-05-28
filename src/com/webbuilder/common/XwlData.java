package com.webbuilder.common;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONObject;

public class XwlData {
	public String parentId;
	public JSONObject content;
	public String title;
	public String icon;
	public String createUser;
	public Date createDate;
	public String lastModifyUser;
	public Date lastModifyDate;
	public ArrayList<String> roles;
	public int orderIndex;
	public boolean isHidden;
	public boolean isFolder;
	public boolean newWin;
}
