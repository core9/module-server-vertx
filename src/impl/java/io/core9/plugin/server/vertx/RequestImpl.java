package io.core9.plugin.server.vertx;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.request.Method;
import io.core9.plugin.server.request.Request;
import io.core9.plugin.server.request.Response;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.net.NetSocket;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.schedulers.Schedulers;

public class RequestImpl implements Request, HttpServerRequest {
	private static HashMap<String, Method> methods = new HashMap<String, Method>();
	{
		methods.put("GET", Method.GET);
		methods.put("PUT", Method.PUT);
		methods.put("POST", Method.POST);
		methods.put("DELETE", Method.DELETE);
		methods.put("OPTIONS", Method.OPTIONS);
		methods.put("HEAD", Method.HEAD);
	}

	HttpServerRequest request;
	private VirtualHost vhost;
	private ResponseImpl response;
	private String path;
	private Method type;
	private Map<String, Object> context;
	private Map<String, Object> params;
	private boolean expectMultiPartCalled;
	private String strBody;
	private Observable<String> body = Observable.create((OnSubscribe<String>) subscriber -> {
		subscriber.onNext(strBody);
		subscriber.onCompleted();
	}).subscribeOn(Schedulers.io());
	private List<Cookie> cookies;

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public Response getResponse() {
		return response;
	}

	public RequestImpl(HttpServerRequest request) {
		this.request = request;
		this.context = new HashMap<String, Object>();
		this.path = request.path();
		this.params = new HashMap<String, Object>();
		for (Map.Entry<String, String> entry : request.params()) {
			this.params.put(entry.getKey(), entry.getValue());
		}
		this.type = methods.get(request.method());
		this.response = new ResponseImpl(request.response());
	}

	@Override
	public Method getMethod() {
		return type;
	}

	@Override
	public HttpServerRequest dataHandler(Handler<Buffer> handler) {
		request.dataHandler(handler);
		return this;
	}

	@Override
	public HttpServerRequest pause() {
		request.pause();
		return this;
	}

	@Override
	public HttpServerRequest resume() {
		request.resume();
		return this;
	}

	@Override
	public HttpServerRequest endHandler(Handler<Void> endHandler) {
		request.endHandler(endHandler);
		return this;
	}

	@Override
	public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
		request.exceptionHandler(handler);
		return this;
	}

	@Override
	public HttpVersion version() {
		return request.version();
	}

	@Override
	public String method() {
		return request.method();
	}

	@Override
	public String uri() {
		return request.uri();
	}

	@Override
	public String path() {
		return request.path();
	}

	@Override
	public String query() {
		return request.query();
	}

	@Override
	public ResponseImpl response() {
		return response;
	}

	@Override
	public MultiMap headers() {
		return request.headers();
	}

	@Override
	public MultiMap params() {
		return request.params();
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return request.remoteAddress();
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return request.peerCertificateChain();
	}

	@Override
	public URI absoluteURI() {
		return request.absoluteURI();
	}

	@Override
	public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
		request.bodyHandler(bodyHandler);
		return this;
	}

	@Override
	public NetSocket netSocket() {
		return request.netSocket();
	}

	@Override
	public HttpServerRequest expectMultiPart(boolean expect) {
		// if we expect
		if (expect) {
			// then only call it once
			if (!expectMultiPartCalled) {
				expectMultiPartCalled = true;
				request.expectMultiPart(expect);
			}
		} else {
			// if we don't expect reset even if we were called before
			expectMultiPartCalled = false;
			request.expectMultiPart(expect);
		}
		return this;
	}

	@Override
	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
		request.uploadHandler(uploadHandler);
		return this;
	}

	@Override
	public MultiMap formAttributes() {
		return request.formAttributes();
	}

	@Override
	public Map<String, Object> getParams() {
		return this.params;
	}

	@SuppressWarnings("unchecked")
    private static Map<String, Object> splitQuery(String query) {
		if(query == null || query.length() == 0) return new HashMap<String, Object>();
		String[] pairs = query.split("&");
		Map<String, Object> query_pairs = new HashMap<String, Object>();
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			try {
				String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
				String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
				Set<String> queryKeys = query_pairs.keySet();
				
				if(!queryKeys.isEmpty() && queryKeys.contains(key)){
					ArrayList<String> paramList = new ArrayList<>();
					Object list = query_pairs.get(key);
					if(list instanceof String){
						paramList.add(value);
					}else if(list instanceof ArrayList){
						paramList.addAll((Collection<? extends String>) query_pairs.get(key));
						paramList.add(value);
					} 
					query_pairs.put(key, paramList);
				}else{
					query_pairs.put(key,value);
				}
				
				
			} catch (UnsupportedEncodingException e) {
				return new HashMap<>();
			}
		}
		return query_pairs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getContext(String name) {
		return (R) this.context.get(name);
	}

	@Override
	public <R> R getContext(String name, R defaultValue) {
		if (context.containsKey(name)) {
			return getContext(name);
		} else {
			return defaultValue;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R putContext(String name, R value) {
		return (R) this.context.put(name, value);
	}

	@Override
	public Map<String, Object> getContext() {
		return this.context;
	}

	public void setBody(String body) {
		this.strBody = body;
	}

	@Override
	public Observable<String> getBody() {
		return body;
	}

	@Override
	public Observable<List<Object>> getBodyAsList() {
		return Observable.create((OnSubscribe<List<Object>>) subscriber -> {
			this.getBody().subscribe((value) -> {
				try {
					subscriber.onNext((JSONArray) JSONValue.parse(value));
				} catch (Exception e) {
					subscriber.onError(e);
				}
			});
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Observable<Map<String, Object>> getBodyAsMap() {
		return Observable.create((OnSubscribe<Map<String,Object>>) subscriber -> {
			this.getBody().subscribe((value) -> {
				try {
					if(this.context.containsKey("is_json_object") && (boolean) this.context.get("is_json_object")) {
						subscriber.onNext(((JSONObject) JSONValue.parse(value)));
					} else {
						subscriber.onNext(splitQuery(value));
					}
					subscriber.onCompleted();
				} catch (Exception e) {
					subscriber.onError(e);
				}
			});
		}).subscribeOn(Schedulers.io());
	}

	public void setVirtualHost(VirtualHost vhost) {
		this.vhost = vhost;
		this.response.setVirtualHost(vhost);
	}

	@Override
	public VirtualHost getVirtualHost() {
		return this.vhost;
	}

	@Override
	public List<Cookie> getAllCookies(String name) {
		List<Cookie> foundCookies = new ArrayList<>();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (name.equals(c.getName())) {
					foundCookies.add(c);
				}
			}
		}
		return foundCookies;
	}

	@Override
	public Cookie getCookie(String name) {
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (name.equals(c.getName()) && ( "/".equals(c.getPath()) || c.getPath() == null )) {
					return c;
				}
			}
		}
		return null;
	}

	@Override
	public void setCookies(List<Cookie> cookies) {
		this.cookies = cookies;
	}

	@Override
	public String getHostname() {
		return request.headers().get("Host");
	}

	@Override
	public InetSocketAddress localAddress() {
		// TODO Auto-generated method stub
		return null;
	}

}