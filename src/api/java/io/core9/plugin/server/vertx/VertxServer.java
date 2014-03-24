package io.core9.plugin.server.vertx;

import io.core9.core.executor.Executor;
import io.core9.core.plugin.Core9Plugin;
import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.Server;

public interface VertxServer extends Core9Plugin, Server, Executor {
	
	/**
	 * Return a new cookie implementation
	 * @return
	 */
	Cookie newCookie(String name, String value);
	
	/**
	 * Return a new cookie implementation
	 * @return
	 */
	Cookie newCookie(String name);

}
