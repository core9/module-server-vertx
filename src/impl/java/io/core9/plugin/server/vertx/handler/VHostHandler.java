package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.vertx.RequestImpl;

import java.util.Map;

public class VHostHandler implements ServerHandler {
	
	private Map<String, VirtualHost> hosts;
	
	public VHostHandler(HostManager hostManager) {
		this.hosts = hostManager.getVirtualHostsByHostname();
	}

	@Override
	public void handle(RequestImpl request, HandlerProcessor processor) {
		VirtualHost vhost = hosts.get(request.getHostname());
		request.setVirtualHost(vhost);
		processor.next();
	}
}
