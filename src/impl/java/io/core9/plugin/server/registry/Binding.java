package io.core9.plugin.server.registry;

import io.core9.plugin.server.handler.Middleware;
import io.core9.plugin.server.vertx.RequestImpl;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Binding {
	final String path;
	final Pattern pattern;
	final Middleware middleware;
	final Set<String> paramNames;
	
	public Binding(String path, Pattern pattern, Set<String> paramNames,Middleware middleware) {
		this.path = path;
		this.pattern = pattern;
		this.middleware = middleware;
		this.paramNames = paramNames;
	}
	
	public void handle(RequestImpl request) {
		Matcher matcher = pattern.matcher(request.getPath());
		if(!matcher.matches()) {
			return;
		} else {
			Map<String, Object> params = request.getParams();
			if (this.paramNames != null) {
				// Named params
				for (String param : paramNames) {
					params.put(param, matcher.group(param));
				}
			} else {
				// Un-named params
				for (int i = 0; i < matcher.groupCount(); i++) {
					params.put("param" + i, matcher.group(i + 1));
				}
			}
			middleware.handle(request);
		}
	}
}
