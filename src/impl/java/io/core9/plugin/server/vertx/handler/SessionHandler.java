package io.core9.plugin.server.vertx.handler;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.vertx.CookieImpl;
import io.core9.plugin.server.vertx.RequestImpl;
import io.netty.handler.codec.http.CookieDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.crypto.Mac;

public class SessionHandler implements ServerHandler {
	
    private final Mac hmacSHA256;
    
    public SessionHandler(Mac hmacSHA256) {
        this.hmacSHA256 = hmacSHA256;
    }

	@Override
	public void handle(RequestImpl request, HandlerProcessor processor) {
		String cookieHeader = request.headers().get("cookie");
		if (cookieHeader != null) {
            Set<io.netty.handler.codec.http.Cookie> nettyCookies = CookieDecoder.decode(cookieHeader);
            List<Cookie> cookies = new ArrayList<>();

            for (io.netty.handler.codec.http.Cookie nettyCookie : nettyCookies) {
                CookieImpl cookie = new CookieImpl(nettyCookie, hmacSHA256);
                cookies.add(cookie);
            }
            request.setCookies(cookies);
        }
        processor.next();
    }
}
