package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.vertx.RequestImpl;

public class HandlerProcessor {
	
	private static ServerHandler[] handlers;
	private RequestImpl request;
	private int current = 0;
	
	public static void setHandlers(ServerHandler[] handlerList) {
		handlers = handlerList;
	}
	
	public HandlerProcessor setRequest(RequestImpl request) {
		this.request = request;
		return this;
	}

	public void next() {
		if(current != handlers.length) {
			handlers[current++].handle(request, this);
		}
	}
	
	public void run() {
		next();
	}
	
}
