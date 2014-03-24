package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.vertx.RequestImpl;

public interface ServerHandler {

	public void handle(RequestImpl request, HandlerProcessor processor);
}
