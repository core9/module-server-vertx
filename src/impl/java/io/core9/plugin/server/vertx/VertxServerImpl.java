package io.core9.plugin.server.vertx;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.handler.Middleware;
import io.core9.plugin.server.registry.Binding;
import io.core9.plugin.server.registry.BindingsRegistry;
import io.core9.plugin.server.vertx.handler.BodyHandler;
import io.core9.plugin.server.vertx.handler.EndHandler;
import io.core9.plugin.server.vertx.handler.FrameworkHandler;
import io.core9.plugin.server.vertx.handler.HandlerProcessor;
import io.core9.plugin.server.vertx.handler.ServerHandler;
import io.core9.plugin.server.vertx.handler.SessionHandler;
import io.core9.plugin.server.vertx.handler.VHostHandler;
import io.core9.plugin.template.closure.ClosureTemplateEngine;

import java.util.List;

import javax.crypto.Mac;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.injections.InjectPlugin;

import org.apache.log4j.Logger;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServerRequest;

@PluginImplementation
public class VertxServerImpl implements VertxServer {
	
	@InjectPlugin
	private ClosureTemplateEngine engine;
	
	@InjectPlugin
	private HostManager manager;
	
	private static final Logger LOG = Logger.getLogger(VertxServerImpl.class);
	private static final BindingsRegistry BINDINGS = BindingsRegistry.getInstance();
	private static final Mac HMAC = Utils.newHmacSHA256("secret here");

	@Override
	public void use(String pattern, Middleware middleware) {
		//FIXME need to log more info such as components in PageModelRegistryImpl
		LOG.info("Registring middleware : " + middleware.toString() + " with pattern : " + pattern);
		BINDINGS.addPattern(pattern, middleware);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void use(VirtualHost vhost, String pattern, Middleware middleware) {
		LOG.info("Registring: " + pattern + " on vhost: " + vhost.getHostname());
		BINDINGS.addPattern((List<Binding>) vhost.getContext("bindings"), pattern, middleware);
	}

	@Override
	public Cookie newCookie(String name, String value) {
		return new CookieImpl(name, value, HMAC);
	}

	@Override
	public Cookie newCookie(String name) {
		return new CookieImpl(name, HMAC);
	}

	@Override
	public void deregister(String pattern) {
		BINDINGS.removePattern(pattern);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deregister(VirtualHost vhost, String pattern) {
		BINDINGS.removePattern((List<Binding>) vhost.getContext("bindings"), pattern);
	}

	@Override
	public void execute() {
		ResponseImpl.setTemplateEngine(engine);		
		Vertx vertx = VertxFactory.newVertx(8113, "localhost");
		HandlerProcessor.setHandlers(new ServerHandler[]{
								new VHostHandler(manager),
								new SessionHandler(HMAC),
								new BodyHandler(),
								new FrameworkHandler(),
								new EndHandler()
								});
		
		for(int i = 0; i < 10; i++) {
			vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>(){
				@Override
				public void handle(HttpServerRequest event) {
					new HandlerProcessor().setRequest(new RequestImpl(event)).run();
				}
			}).listen(8080);
		}
	}
}
