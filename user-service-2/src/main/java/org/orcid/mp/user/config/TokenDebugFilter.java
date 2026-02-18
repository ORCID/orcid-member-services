package org.orcid.mp.user.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TokenDebugFilter implements Filter {

    private final Logger LOG = LoggerFactory.getLogger(TokenDebugFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String header = req.getHeader("Authorization");

        if (header != null) {
            // BE CAREFUL: This logs full tokens to your console!
            // Only use for local debugging.
            LOG.info(">>> Incoming Auth Header: {}", header);
        } else {
            LOG.info(">>> Incoming Request to {} has NO Authorization header", req.getRequestURI());
        }

        chain.doFilter(request, response);
    }
}
