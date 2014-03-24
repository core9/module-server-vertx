package io.core9.plugin.server.vertx.handler;


import io.core9.plugin.server.request.FileUpload;
import io.core9.plugin.server.request.Method;
import io.core9.plugin.server.vertx.FileUploadImpl;
import io.core9.plugin.server.vertx.RequestImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.DecodeException;

public class BodyHandler implements ServerHandler {
	
	/** 
	 * Internal method to parse JSON requests
	 * @param request
	 * @param buffer
	 * @param next
	 */
    private void parseJson(final RequestImpl request, final Buffer buffer, final HandlerProcessor processor) {
        try {
            String content = buffer.toString();
            if (content.length() > 0) {
                switch (content.charAt(0)) {
                    case '{':
                    	request.putContext("is_json_object", true);
                        request.setBody(content);
                        processor.next();
                        break;
                    case '[':
                    	request.putContext("is_json_array", true);
                        request.setBody(content);
                        processor.next();
                        break;
                    default:
                        request.getResponse().setStatusCode(500);
                        request.getResponse().setStatusMessage("No valid JSON input given");
                        break;
                }
            } else {
                processor.next();
            }
        } catch (DecodeException ex) {
            processor.next();
        }
    }
    

	


	@Override
	public void handle(final RequestImpl request, final HandlerProcessor processor) {
		if(!request.getMethod().equals(Method.POST) && !request.getMethod().equals(Method.PUT)) {
			processor.next();
			return;
		}
		
		MultiMap headers = ((HttpServerRequest) request).headers();
		if(!headers.contains("transfer-encoding") && !headers.contains("content-length")) {
			processor.next();
			return;
		}
		
		final String contentType = request.headers().get("content-type");
		
		final boolean isJSON = contentType != null && contentType.contains("application/json");
		// TODO
        final boolean isMULTIPART = contentType != null && contentType.contains("multipart/form-data");
        // TODO
        final boolean isURLENCODEC = contentType != null && contentType.contains("application/x-www-form-urlencoded");
        
        //FIXME added buffer because post hits null
        final Buffer buffer = (!isMULTIPART && !isURLENCODEC) ? new Buffer(0) : new Buffer(0)/*null*/;

        if(isMULTIPART) {
        	request.expectMultiPart(true);
        	request.uploadHandler(new Handler<HttpServerFileUpload>(){
				@SuppressWarnings("unchecked")
				@Override
				public void handle(HttpServerFileUpload event) {
					if(request.getContext("files") == null) {
						request.putContext("files", new ArrayList<FileUpload>());
					}
					String fs = System.getProperty("java.io.tmpdir");
					if(!fs.endsWith(File.separator)) {
						fs += File.separator;
					}
					String tmpName = fs + UUID.randomUUID().toString();
					((List<FileUpload>) request.getContext("files"))
						.add(new FileUploadImpl(event.name() + event.filename(), event.contentType(), tmpName));
					event.streamToFileSystem(tmpName);
				}
			});
        }

		request.dataHandler(new Handler<Buffer>(){
			@Override
			public void handle(Buffer event) {
				if(!isMULTIPART) {
					buffer.appendBuffer(event);
				}
			}
		});
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void event) {
				if(isJSON) {
					parseJson(request, buffer, processor);
				} else {
					request.setBody(buffer.toString());
					processor.next();
				}
			}
		});
	}
}
