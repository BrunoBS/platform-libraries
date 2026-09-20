package br.com.portalmanager.core.authorization.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PayloadErrorLoggingFilter extends OncePerRequestFilter implements Ordered {

    private static final String MDC_REQUEST_BODY_KEY = "requestBody";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        int umMegabyteEmBytes = 1024 * 1024;
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, umMegabyteEmBytes);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            int status = response.getStatus();
            if (status >= 400) {
                String body = getPayloadFromBody(wrappedRequest);
                if (!body.isBlank()) {
                    MDC.put(MDC_REQUEST_BODY_KEY, body);
                }
            }
        }
    }

    private String getPayloadFromBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                // Verifica se o tamanho do cache atingiu o limite máximo configurado (1 MB)
                boolean cacheAtingiuOLimite = buf.length >= (1024 * 1024);

                String content = new String(buf, 0, buf.length, StandardCharsets.UTF_8);
                content = content.replaceAll("\\s+", " ").trim();

                // Se estourou, adiciona um sufixo explicativo no JSON do log
                return cacheAtingiuOLimite ? content + " ... [PAYLOAD TRUNCATED - EXCEEDED 1MB LIMIT]" : content;
            } catch (Exception ignored) {
                return "[unreadable-payload]";
            }
        }
        return "";
    }

    @Override
    public int getOrder() {
        // Roda com prioridade altíssima na entrada de Servlets, logo após os filtros primitivos
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
