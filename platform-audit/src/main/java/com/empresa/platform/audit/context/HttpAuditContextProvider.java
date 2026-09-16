package com.empresa.platform.audit.context;

import com.empresa.platform.audit.model.AuditContext;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;

public final class HttpAuditContextProvider implements AuditContextProvider {

    private final HttpServletRequest request;

    public HttpAuditContextProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public AuditContext currentContext() {
        Principal principal = request.getUserPrincipal();
        String actor = principal != null ? principal.getName() : "unknown";

        return new AuditContext(
                request.getHeader("X-Account-Id"),
                request.getHeader("X-Application-Id"),
                request.getHeader("X-Environment"),
                actor,
                request.getHeader("X-Correlation-Id")
        );
    }
}
