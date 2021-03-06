package io.core9.plugin.server.vertx;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.request.Response;
import io.core9.plugin.template.TemplateEngine;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerResponse;

public class ResponseImpl implements Response, HttpServerResponse {
	// the original request
	private VirtualHost vhost;
	private final HttpServerResponse response;
	private static TemplateEngine<String> templateEngine;

	private String template;
	private Map<String, Object> values;
	private Map<String, Object> globals;
	private boolean ended = false;
	private List<CookieImpl> cookies;

	public static void setTemplateEngine(TemplateEngine<String> engine) {
		templateEngine = engine;
	}

	@Override
	public String getTemplate() {
		return template;
	}

	@Override
	public ResponseImpl setTemplate(String template) {
		this.template = template;
		return this;
	}

	@Override
	public Map<String, Object> getValues() {
		return values;
	}

	@Override
	public Response addValues(Map<String, Object> values) {
		this.values.putAll(values);
		return this;
	}

	@Override
	public Response addValue(String key, Object value) {
		this.values.put(key, value);
		return this;
	}

	@Override
	public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
		response.setWriteQueueMaxSize(maxSize);
		return this;
	}
	
	public ResponseImpl setVirtualHost(VirtualHost vhost) {
		this.vhost = vhost;
		this.addGlobal("hostname", vhost.getHostname());
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return response.writeQueueFull();
	}

	@Override
	public HttpServerResponse drainHandler(Handler<Void> handler) {
		response.drainHandler(handler);
		return this;
	}

	@Override
	public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
		response.exceptionHandler(handler);
		return this;
	}

	@Override
	public String getStatusMessage() {
		return response.getStatusMessage();
	}

	@Override
	public ResponseImpl setStatusMessage(String statusMessage) {
		response.setStatusMessage(statusMessage);
		return this;
	}

	@Override
	public HttpServerResponse setChunked(boolean chunked) {
		response.setChunked(chunked);
		return this;
	}

	@Override
	public boolean isChunked() {
		return response.isChunked();
	}

	@Override
	public MultiMap headers() {
		return response.headers();
	}

	@Override
	public ResponseImpl putHeader(String name, String value) {
		try {
			response.putHeader(name, value);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("java.lang.IllegalStateException: Response has already been written"
			        + this.getClass().getCanonicalName() + " : putHeader");
		}
		return this;
	}

	@Override
	public HttpServerResponse putHeader(String name, Iterable<String> values) {
		response.putHeader(name, values);
		return this;
	}

	@Override
	public MultiMap trailers() {
		return response.trailers();
	}

	@Override
	public HttpServerResponse putTrailer(String name, String value) {
		response.putTrailer(name, value);
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(String name, Iterable<String> values) {
		response.putTrailer(name, values);
		return this;
	}

	@Override
	public HttpServerResponse closeHandler(Handler<Void> handler) {
		response.closeHandler(handler);
		return this;
	}

	@Override
	public HttpServerResponse write(Buffer chunk) {
		response.write(chunk);
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk, String enc) {
		response.write(chunk, enc);
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk) {
		response.write(chunk);
		return this;
	}

	@Override
	public void end(String chunk) {
		processHeaders();
		response.end(chunk);
		this.ended = true;
	}

	@Override
	public void end(String chunk, String enc) {
		processHeaders();
		response.end(chunk, enc);
		this.ended = true;
	}

	@Override
	public void end(Buffer chunk) {
		processHeaders();
		response.end(chunk);
		this.ended = true;
	}
	
	@Override
	public boolean isEnded() {
		return ended;
	}

	@Override
	public void end() {
		if (!this.ended) {
			processHeaders();
			if(this.template != null) {
				String result = "";
				try {
					result = processTemplate();
				} catch (Exception e) {
					result = e.getMessage();
				}
				response.end(result);
			} else if (template == null && values.size() > 0) {
				sendJsonMap(values);
			} else {
				response.end();
			}
		}
		this.ended = true;
	}

	@Override
	public ResponseImpl sendFile(String filename) {
		processHeaders();
		response.sendFile(filename);
		this.ended = true;
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename, String notFoundFile) {
		processHeaders();
		response.sendFile(filename, notFoundFile);
		this.ended = true;
		return this;
	}

	@Override
	public void close() {
		response.close();
	}

	@Override
	public int getStatusCode() {
		return response.getStatusCode();
	}

	@Override
	public ResponseImpl setStatusCode(int statusCode) {
		response.setStatusCode(statusCode);
		return this;
	}

	@Override
	public ResponseImpl sendBinary(byte[] bin) {
		processHeaders();
		response.end(new Buffer(bin));
		this.ended = true;
		return this;
	}

	@Override
	public void sendJsonArray(List<? extends Object> list) {
		processHeaders();
		response.headers().add("Content-Type", "application/json");
		response.end(JSONArray.toJSONString(list));
		this.ended = true;
	}

	@Override
	public void sendJsonArray(Set<? extends Object> list) {
		processHeaders();
		response.headers().add("Content-Type", "application/json");
		response.end(JSONArray.toJSONString(new ArrayList<Object>(list)));
		this.ended = true;
	}

	@Override
	public void sendJsonMap(Map<String, Object> map) {
		processHeaders();
		response.headers().add("Content-Type", "application/json");
		response.end(JSONObject.toJSONString(map));
		this.ended = true;
	}
	
	@Override
	public void sendRedirect(int status, String url) {
		processHeaders();
		response.setStatusCode(status);
		response.headers().add("Content-Type", "plain/text");
		response.headers().add("Location", url);
		response.end();
		this.ended = true;
	}

	@Override
	public Response addCookie(Cookie cookie) {
		CookieImpl c = (CookieImpl) cookie;
		if (cookies == null) {
			cookies = new ArrayList<>();
		}
		cookies.add(c);
		return this;
	}
	
	/**
	 * Process the headers
	 */
	private void processHeaders() {
		if (this.cookies != null) {
			List<io.netty.handler.codec.http.Cookie> nettyCookies = new ArrayList<io.netty.handler.codec.http.Cookie>();
			for (CookieImpl cookie : this.cookies) {
				nettyCookies.add((io.netty.handler.codec.http.Cookie) cookie);
			}
			response.putHeader("set-cookie", ServerCookieEncoder.encode(nettyCookies));
		}
	}
	
	private String processTemplate() throws Exception {
		String contentType = response.headers().get("Content-Type");
		if(contentType == null) {
			// Default to text/html content type
			response.headers().add("Content-Type", "text/html");
		}
		return templateEngine.render(vhost, template, values, globals);
	}

	@Override
	public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
		response.putHeader(name, value);
		return this;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
		response.putHeader(name, values);
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
		response.putTrailer(name, value);
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence name,	Iterable<CharSequence> values) {
		response.putTrailer(name, values);
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename,	Handler<AsyncResult<Void>> resultHandler) {
		response.sendFile(filename, resultHandler);
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename, String notFoundFile, Handler<AsyncResult<Void>> resultHandler) {
		response.sendFile(filename, notFoundFile, resultHandler);
		return this;
	}

	@Override
	public Response addGlobal(String key, Object value) {
		if(this.globals == null) {
			this.globals = new HashMap<String,Object>();
		}
		this.globals.put(key, value);
		return this;
	}

	@Override
	public Map<String, Object> getGlobals() {
		return this.globals;
	}
	
	public ResponseImpl(HttpServerResponse response) {
		this.response = response;
		this.values = new HashMap<String, Object>();
	}
}
