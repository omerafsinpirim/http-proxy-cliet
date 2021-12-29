package com.github.emotion.httpproxy;

import com.github.emotion.httpproxy.handler.ExceptionHandler;
import com.github.emotion.httpproxy.handler.HttpProxyRequestHandler;
import com.github.emotion.httpproxy.handler.HttpProxyResponseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

public final class HttpProxyClient {
    private HttpProxyURI targetURI;
    private HttpClient httpClient;
    private HttpProxyRequestHandler httpProxyRequestHandler;
    private HttpProxyResponseHandler httpProxyResponseHandler;
    private ExceptionHandler exceptionHandler;

    HttpProxyClient(URI targetURI, HttpClient httpClient, HttpProxyRequestHandler httpProxyRequestHandler
            , HttpProxyResponseHandler httpProxyResponseHandler, ExceptionHandler exceptionHandler) {
        this.targetURI = new HttpProxyURI(targetURI);
        this.httpClient = httpClient;
        this.httpProxyRequestHandler = httpProxyRequestHandler;
        this.httpProxyResponseHandler = httpProxyResponseHandler;
        this.exceptionHandler = exceptionHandler;
    }

    public void process(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        HttpResponse httpResponse = null;
        HttpUriRequest httpUriRequest = null;
        try {
            httpUriRequest = httpProxyRequestHandler.interpret(httpServletRequest, targetURI);
            if (httpUriRequest == null) {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            httpResponse = httpClient.execute(httpUriRequest);
            httpProxyResponseHandler.interpret(httpServletRequest, httpServletResponse, httpResponse, targetURI);
        } catch (Exception e) {
            exceptionHandler.handle(e, httpServletRequest, httpServletResponse, httpUriRequest, httpResponse, targetURI);
        } finally {
            if (httpResponse != null) {
                consumeQuietly(httpResponse.getEntity());
            }
            if (httpUriRequest != null && !httpUriRequest.isAborted()) {
                httpUriRequest.abort();
            }
        }
    }
 
    private void consumeQuietly(HttpEntity entity) {
        try {
            EntityUtils.consume(entity);
        } catch (IOException e) {//ignore
//            log(e.getMessage(), e);
        }
    }

    public void close() {
      
        if (httpClient instanceof Closeable) {
            try {
                ((Closeable) httpClient).close();
            } catch (IOException e) {
            }
        } else {
           
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

}
