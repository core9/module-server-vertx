package io.core9.plugin.server.registry;

import io.core9.plugin.server.handler.Middleware;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BindingsRegistry {
	private static BindingsRegistry singleton;
	private List<Binding> bindings = new CopyOnWriteArrayList<Binding>();
	
	public static BindingsRegistry getInstance() {
		if(singleton == null) {
			singleton = new BindingsRegistry();
		}
		return singleton;
	}
	private BindingsRegistry() {};
	
	public List<Binding> getBindings() {
		return this.bindings;
	}
		
	/**
	 * Add a new pattern to the routing system
	 * 
	 * @param input
	 * @param plugin
	 * @param bindings
	 */
	public void addPattern(List<Binding> list, String input, Middleware middleware) {
		// We need to search for any :<token name> tokens in the String and
		// replace them with named capture groups
		Matcher m = Pattern.compile(":([A-Za-z][A-Za-z0-9_-]*)").matcher(input);
		StringBuffer sb = new StringBuffer();
		Set<String> groups = new HashSet<>();
		while (m.find()) {
			String group = m.group().substring(1);
			if (groups.contains(group)) {
				throw new IllegalArgumentException("Cannot use identifier "
						+ group + " more than once in pattern string");
			}
			m.appendReplacement(sb, "(?<$1>[^\\/]+)");
			groups.add(group);
		}
		m.appendTail(sb);
		String regex = sb.toString();
		list.add(new Binding(input, Pattern.compile(regex), groups, middleware));
	}
	
	/**
	 * Add a new pattern to the routing system
	 * 
	 * @param input
	 * @param plugin
	 * @param bindings
	 */
	public void addPattern(String input, Middleware middleware) {
		addPattern(bindings, input, middleware);
	}
	
	/**
	 * Remove a binding
	 * @param pattern
	 */
	public void removePattern(String pattern) {
		removePattern(bindings, pattern);
	}
	
	/**
	 * Remove a binding
	 * @param bindings
	 * @param pattern
	 */
	public void removePattern(List<Binding> bindings, String pattern) {
		for(Binding binding: bindings) {
			if (binding.path.equals(pattern)) {
				bindings.remove(binding);
			}
		}
	}
}
