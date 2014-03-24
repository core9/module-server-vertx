package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.registry.Binding;
import io.core9.plugin.server.vertx.RequestImpl;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class VHostHandler implements ServerHandler {
	
	private Map<String, VirtualHost> hosts;
	private HostManager manager;
	
	public VHostHandler(HostManager hostManager) {
		this.manager = hostManager; 
		this.hosts = hostManager.getVirtualHostsByHostname();
	}

	@Override
	public void handle(RequestImpl request, HandlerProcessor processor) {
		VirtualHost vhost = hosts.get(request.getHostname());
		// TODO Factor out to vhost module
		if(vhost == null) {
			vhost = retryVirtualHost(request.getHostname());
			if(vhost == null) {
				vhost = createNewVirtualHost(request);
			}
		}
		if(vhost != null) {
			request.setVirtualHost(vhost);
			processor.next();
		}
	}
	
	private VirtualHost retryVirtualHost(String hostname) {
		this.hosts = manager.getVirtualHostsByHostname();
		return hosts.get(hostname);
	}
	
	private VirtualHost createNewVirtualHost(RequestImpl request) {
		if(request.getPath().equals("/install")) {
			VirtualHost vhost = new VirtualHost(request.getHostname());
			vhost.putContext("newhost", true);
			vhost.putContext("bindings", new CopyOnWriteArrayList<Binding>());
			return vhost;
		} else {
			request.getResponse().end("Host unknown, visit /install if the admin module is available.");
			return null;
		}
	}
}
