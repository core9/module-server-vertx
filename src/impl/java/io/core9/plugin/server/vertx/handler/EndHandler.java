package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.request.Response;
import io.core9.plugin.server.vertx.RequestImpl;

public class EndHandler implements ServerHandler {
	
	private HostManager manager;
	
	public EndHandler(HostManager manager) {
		this.manager = manager;
	}

	@Override
	public void handle(RequestImpl request, HandlerProcessor processor) {
		Response response = request.getResponse();
		if(!response.isEnded()) {
			if(response.getTemplate() != null || response.getValues().size() > 0) {
				response.end();
			} else {
				String alias = manager.getURLAlias(request.getVirtualHost(), request.getPath());
				if(alias != null) {
					response.sendRedirect(307, alias);
				} else {
					//response.setStatusCode(404);
					response.end("Not found");
				}
			}
		}
	}
}
