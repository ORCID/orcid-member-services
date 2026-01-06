package org.orcid.mp.user.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            var incomingRequest = attributes.getRequest();
            String authHeader = incomingRequest.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                request.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        }
        return execution.execute(request, body);
    }
}
