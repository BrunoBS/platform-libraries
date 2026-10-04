package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.field.AuditFieldResolver;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditAspectSpringProxyTest {

    @Test
    void shouldPublishThroughSpringProxyForAnAnnotatedBusinessMethod() {
        try (var context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            when(context.getBean(AuditAuthorizationContextResolver.class).resolve())
                    .thenReturn(new AuditContext("account-1", "application-1", "dev", "user-1", "trace-1"));

            ResponseEntity<Map<String, String>> response =
                    context.getBean(TestService.class).update();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(context.getBean(PublishedEvents.class).events)
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.resourceId()).isEqualTo("resource-1");
                        assertThat(event.action()).isEqualTo("UPDATE");
                    });
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfiguration {
        @Bean PlatformAuditProperties properties() {
            PlatformAuditProperties properties = new PlatformAuditProperties();
            properties.setServiceName("proxy-test");
            return properties;
        }

        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean HttpServletRequest request() { return mock(HttpServletRequest.class); }
        @Bean AuditAuthorizationContextResolver contextResolver() {
            return mock(AuditAuthorizationContextResolver.class);
        }
        @Bean AuditFieldResolver fieldResolver(
                PlatformAuditProperties properties,
                ObjectMapper objectMapper,
                HttpServletRequest request
        ) {
            return new AuditFieldResolver(properties, objectMapper, request);
        }
        @Bean AuditEventFactory eventFactory(
                PlatformAuditProperties properties,
                AuditAuthorizationContextResolver resolver,
                AuditFieldResolver fieldResolver,
                ObjectMapper objectMapper
        ) {
            return new AuditEventFactory(properties, resolver, fieldResolver, objectMapper);
        }
        @Bean AuditPublisher publisher(PublishedEvents publishedEvents) {
            return publishedEvents.events::add;
        }
        @Bean PublishedEvents publishedEvents() { return new PublishedEvents(); }
        @Bean AuditAspect auditAspect(
                PlatformAuditProperties properties,
                AuditEventFactory factory,
                AuditPublisher publisher
        ) {
            return new AuditAspect(properties, factory, publisher);
        }
        @Bean TestService testService() { return new TestService(); }
    }

    static class PublishedEvents {
        final List<AuditEventRequest> events = new ArrayList<>();
    }

    static class TestService {
        @Auditable(
                resource = "account",
                action = "UPDATE",
                resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "id")
        )
        public ResponseEntity<Map<String, String>> update() {
            return ResponseEntity.ok(Map.of("id", "resource-1"));
        }
    }
}
