package com.webbuilder.common;

import java.io.File;
import java.io.FileReader;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.webbuilder.utils.StringUtil;

public class ScriptBuffer {
	private static ScriptEngine engine;
	private static Compilable compilable;
	private static ConcurrentHashMap<String, CompiledScript> scriptMap;

	public static void run(String id, String scriptText,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		if (scriptMap == null)
			initialize(false);
		CompiledScript script = scriptMap.get(id);
		if (script == null) {
			script = compilable.compile(StringUtil.concat("(function(){",
					scriptText, "\n})();"));
			scriptMap.put(id, script);
		}
		Bindings bindings = engine.createBindings();
		bindings.put("request", request);
		bindings.put("response", response);
		script.eval(bindings);
	}

	public static void remove(String id) throws Exception {
		if (scriptMap == null)
			return;
		Set<Entry<String, CompiledScript>> es = scriptMap.entrySet();
		String k;

		for (Entry<String, CompiledScript> e : es) {
			k = e.getKey();
			if (k.startsWith(id + "."))
				scriptMap.remove(k);
		}
	}

	public static synchronized void initialize(boolean reload) throws Exception {
		if (!reload && scriptMap != null)
			return;
		ScriptEngineManager manager = new ScriptEngineManager();
		engine = manager.getEngineByName("javascript");
		compilable = (Compilable) engine;
		Bindings globalBindings = engine
				.getBindings(ScriptContext.GLOBAL_SCOPE);
		CompiledScript wbScript = compilable.compile(new FileReader(new File(
				Main.path, "webbuilder/script/server.js")));
		wbScript.eval(globalBindings);
		scriptMap = new ConcurrentHashMap<String, CompiledScript>();
	}
}