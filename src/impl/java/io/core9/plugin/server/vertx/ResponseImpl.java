package io.core9.plugin.server.vertx;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.request.Response;
import io.core9.plugin.template.TemplateEngine;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ResponseImpl implements Response, HttpServerResponse {
	// the original request
	private final HttpServerResponse response;
	private static TemplateEngine<String> templateEngine;

	private String template;
	private Map<String, Object> values;
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
	public void addValues(Map<String, Object> values) {
		this.values.putAll(values);
	}

	@Override
	public void addValue(String key, Object value) {
		this.values.put(key, value);
	}

	public ResponseImpl(HttpServerResponse response) {
		this.response = response;
		values = new HashMap<String, Object>();
	}

	@Override
	public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
		response.setWriteQueueMaxSize(maxSize);
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

		try {
			response.end(chunk);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("java.lang.IllegalStateException: Response has already been written"
			        + this.getClass().getCanonicalName() + " end() ");
		}

		this.ended = true;
	}

	@Override
	public void end(String chunk, String enc) {
		response.end(chunk, enc);
		this.ended = true;
	}

	@Override
	public void end(Buffer chunk) {
		response.end(chunk);
		this.ended = true;
	}

	@Override
	public void end() {
		if (!this.ended) {
			if (this.cookies != null) {
				// TODO Fix
				List<io.netty.handler.codec.http.Cookie> nettyCookies = new ArrayList<io.netty.handler.codec.http.Cookie>();
				for (CookieImpl cookie : this.cookies) {
					nettyCookies.add((io.netty.handler.codec.http.Cookie) cookie);
				}
				response.putHeader("set-cookie", ServerCookieEncoder.encode(nettyCookies));
			}
			// FIXME
			if (this.template != null && templateEngine != null) {
				try {
					String contentType = response.headers().get("Content-Type");
					if (contentType == null) {
						String content = templateEngine.render(template, values);
						// if render fails then don't set content type
						response.headers().add("Content-Type", "text/html");
						response.end(content);
					}
					
				} catch (Exception e) {
					response.headers().add("Content-Type", "text/plain");
					response.end(e.getMessage());
				}
			} else if (values.containsKey("filename")) {
				response.sendFile((String) values.get("filename"));
			} else if (values.containsKey("bin")) {
				sendBinary((byte[]) values.get("bin"));
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
		response.sendFile(filename);
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename, String notFoundFile) {
		response.sendFile(filename, notFoundFile);
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
		response.end(new Buffer(bin));
		this.ended = true;
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void sendJsonArray(List<? extends Object> list) {
		response.headers().add("Content-Type", "application/json");
		response.end(new JsonArray((List<Object>) list).encodePrettily());
		this.ended = true;
	}

	@Override
	public void sendJsonArray(Set<? extends Object> list) {
		response.headers().add("Content-Type", "application/json");
		response.end(new JsonArray(list.toArray()).encodePrettily());
		this.ended = true;
	}

	@Override
	public void sendJsonMap(Map<String, Object> map) {
		response.headers().add("Content-Type", "application/json");
		response.end(new JsonObject(map).toString());
		this.ended = true;
	}
	
	@Override
	public void sendRedirect(int status, String url) {
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
}
