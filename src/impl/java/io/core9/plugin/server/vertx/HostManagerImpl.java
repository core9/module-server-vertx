package io.core9.plugin.server.vertx;

import io.core9.core.boot.CoreBootStrategy;
import io.core9.plugin.database.mongodb.MongoDatabase;
import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.VirtualHostProcessor;
import io.core9.plugin.server.handler.Middleware;
import io.core9.plugin.server.registry.Binding;
import io.core9.plugin.server.request.Request;
import io.core9.plugin.template.closure.ClosureTemplateEngine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import net.xeoh.plugins.base.Plugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.injections.InjectPlugin;

import org.apache.commons.lang3.ClassUtils;

import com.google.common.io.CharStreams;

@PluginImplementation
public class HostManagerImpl extends CoreBootStrategy implements HostManager {
	
	private static final String VHOST_COLLECTION = "virtualhosts";

	@InjectPlugin
	private MongoDatabase database;
	
	@InjectPlugin
	private ClosureTemplateEngine engine;

	@Override
	public HostManager addVirtualHost(VirtualHost vhost) throws UnknownHostException {
		createVirtualHostDatabase(vhost);
		Map<String, Object> item = new HashMap<String,Object>();
		item.put("hostname", vhost.getHostname());
		item.put("context", vhost.getContext());
		database.upsert(database.getMasterDBName(), VHOST_COLLECTION, item, item);
		vhost.putContext("bindings", new CopyOnWriteArrayList<Binding>());
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public VirtualHost[] getVirtualHosts() {
		
		List<Map<String,Object>> vhosts = null;
		try {
			vhosts = database.getMultipleResults(database.getMasterDBName(), VHOST_COLLECTION, new HashMap<String,Object>());
        } catch (Exception e) {
        	e.printStackTrace();
        }
		
		VirtualHost[] vhostArray = new VirtualHost[vhosts.size()];
		for(int i = 0; i < vhosts.size(); i++) {
			Map<String,Object> vhostMap = vhosts.get(i);
			VirtualHost vhost = new VirtualHost((String) vhostMap.get("hostname"));
			vhost.setContext((Map<String, Object>) vhostMap.get("context"));
			vhost.putContext("bindings", new CopyOnWriteArrayList<Binding>());
			vhostArray[i] = vhost;
		}
		return vhostArray;
	}

	@Override
	public Map<String, VirtualHost> getVirtualHostsByHostname() {
		Map<String, VirtualHost> hosts = new HashMap<String, VirtualHost>();
		for(VirtualHost vhost : getVirtualHosts()) {
			hosts.put(vhost.getHostname(), vhost);
		}
		return hosts;
	}

	@Override
	public void processPlugins() {
		try {
			engine.addString("io.core9.admin.install.soy", CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/templates/install.soy"))));
			engine.createCache();
		} catch (IOException e) {
			e.printStackTrace();
		}
		VirtualHost[] vhosts = getVirtualHosts();
		for(VirtualHost vhost : vhosts) {
			try {
				createVirtualHostDatabase(vhost);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		setVirtualHostsOnPlugins(vhosts);
	}
	
	/**
	 * Create database references for vitualhosts
	 * @param vhost
	 */
	private void createVirtualHostDatabase(VirtualHost vhost) throws UnknownHostException {
		if(vhost.getContext("dbhost") != null && !vhost.getContext("dbhost").equals("")) {
			database.addDatabase((String) vhost.getContext("dbhost"), (String) vhost.getContext("database"), (String) vhost.getContext("dbuser"), (String) vhost.getContext("password"));
		} else {
			database.addDatabase((String) vhost.getContext("database"), (String) vhost.getContext("dbuser"), (String) vhost.getContext("password"));
		}
	}

	private void setVirtualHostsOnPlugins(VirtualHost[] vhosts) {
		for (Plugin plugin : this.registry.getPlugins()) {
			List<Class<?>> interfaces = ClassUtils.getAllInterfaces(plugin.getClass());
			if (interfaces.contains(VirtualHostProcessor.class)) {
				((VirtualHostProcessor) plugin).process(vhosts);
			}
		}
	}
	
	@Override
	public Integer getPriority() {
		return 320;
	}

	@Override
	public Middleware getInstallationProcedure() {
		return new Middleware() {
			@Override
			public void handle(Request request) {
				if(request.getVirtualHost().getContext("newhost", false)) {
					switch(request.getMethod()) {
					case POST:
						Map<String,Object> body = request.getBodyAsMap();
						try {
							addVirtualHost(parseContext(request.getVirtualHost(), body));
							request.getResponse().setTemplate("io.core9.admin.installed");
						} catch (UnknownHostException e) {
							request.getResponse().setStatusCode(500);
							request.getResponse().end("Error:" + e.getMessage());
						}
						break;
					case GET:
					default:
						request.getResponse().addValue("hostname", request.getHostname());
						request.getResponse().setTemplate("io.core9.admin.install");
						break;
					}
				} else {
					request.getResponse().setTemplate("io.core9.admin.alreadyinstalled");
				}
			}
		};
	}

	/**
	 * Setup a new virtualhost
	 * @param vhost
	 * @param body
	 * @throws UnknownHostException 
	 */
	private VirtualHost parseContext(VirtualHost vhost, Map<String, Object> body) {
		Map<String,Object> context = new HashMap<String,Object>();
		context.put("prefix", body.get("prefix"));
		context.put("database", (String) body.get("database"));
		context.put("dbuser", (String) body.get("username"));
		context.put("password", (String) body.get("password"));
		context.put("dbhost", (String) body.get("dbhost"));
		vhost.setContext(context);
		return vhost;
	}
}
