package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.vertx.RequestImpl;

public class EndHandler implements ServerHandler {

	@Override
	public void handle(RequestImpl request, HandlerProcessor processor) {
		request.response().end();
	}

}
