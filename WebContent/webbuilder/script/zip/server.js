var Path=com.webbuilder.common.Main.path, 
	Resource=com.webbuilder.common.Resource,
    Str=com.webbuilder.common.Str,
    Value=com.webbuilder.common.Value,
    Var=com.webbuilder.common.Var,
    Encrypter=com.webbuilder.tool.Encrypter,
    DateUtil=com.webbuilder.utils.DateUtil,
    DbUtil=com.webbuilder.utils.DbUtil,
    FileUtil=com.webbuilder.utils.FileUtil,
    JsonUtil=com.webbuilder.utils.JsonUtil,
    LogUtil=com.webbuilder.utils.LogUtil,
    StringUtil=com.webbuilder.utils.StringUtil,
    SysUtil=com.webbuilder.utils.SysUtil,
    WebUtil=com.webbuilder.utils.WebUtil,
    ZipUtil=com.webbuilder.utils.ZipUtil,
    Integer=java.lang.Integer,
    Long=java.lang.Long,
    Float=java.lang.Float,
    Double=java.lang.Double,
    JavaDate=java.util.Date,
    Timestamp=java.sql.Timestamp,
    JavaString=java.lang.String,
    StringBuilder=java.lang.StringBuilder,
    File=java.io.File;
	Now=java.lang.System.currentTimeMillis,
	Wb = {
	print : function(request, o) {
		WebUtil.print(request, o);
	},
	println : function(request, o) {
		WebUtil.println(request, o);
	},
	error : function(o) {
		SysUtil.error(o);
	},
	format : function() {
		return Str.langFormat(arguments[0].getAttribute("sys.lang"),
				arguments[1], [].slice.call(arguments, 2));
	},
	isEmpty : function(v) {
		return v === null || v === undefined || v === '';
	},
	indexOf : function(list, s) {
		var i, j = list.length;
		for (i = 0; i < j; i++)
			if (list[i] === s)
				return i;
		return -1;
	},
	encode : function(o) {
		var a, i, j;
		if (o === null || o === undefined) {
			return 'null';
		} else if (o instanceof Date) {
			return (new Timestamp(o.getTime())).toString();
		} else if (o instanceof JavaDate) {
			return DateUtil.toString(o);
		} else if (typeof o == 'number') {
			return isFinite(o) ? String(o) : 'null';
		} else if (typeof o === 'boolean') {
			return String(o);
		} else if (toString.call(o) === '[object Array]') {
			a = new StringBuilder('[');
			j = o.length;
			for (i = 0; i < j; i++) {
				if (i > 0)
					a.append(',');
				a.append(Wb.encode(o[i]));
			}
			a.append(']');
			return a.toString();
		} else if (toString.call(o) === '[object Object]') {
			a = new StringBuilder('{');
			j = false;
			for (i in o) {
				if (j)
					a.append(',');
				else
					j = true;
				a.append(Wb.encode(i));
				a.append(':');
				a.append(Wb.encode(o[i]));
			}
			a.append('}');
			return a.toString();
		} else
			return StringUtil.encode(o);
	},
	decode : function(s, safe) {
		try {
			return eval('(' + s + ')');
		} catch (e) {
			if (safe)
				return null;
			throw e;
		}
	}
};