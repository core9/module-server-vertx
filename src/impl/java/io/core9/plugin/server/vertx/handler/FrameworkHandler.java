package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.registry.Binding;
import io.core9.plugin.server.registry.BindingsRegistry;
import io.core9.plugin.server.vertx.RequestImpl;

import java.util.List;

public class FrameworkHandler implements ServerHandler {

	private static final List<Binding> BINDINGS = BindingsRegistry.getInstance().getBindings();

	@SuppressWarnings("unchecked")
	@Override
	public void handle(RequestImpl request, HandlerProcessor processor) {
		for(Binding binding : BINDINGS) {
			binding.handle(request);
		}
		for(Binding binding : (List<Binding>) request.getVirtualHost().getContext("bindings")) {
			binding.handle(request);
		}
		processor.next();
	}
}
