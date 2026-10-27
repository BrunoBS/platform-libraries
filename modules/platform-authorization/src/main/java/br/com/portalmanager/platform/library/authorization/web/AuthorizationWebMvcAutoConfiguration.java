package br.com.portalmanager.platform.library.authorization.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;

/** HTTP adapter; other entrypoint transports can provide their own context adapter. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
public class AuthorizationWebMvcAutoConfiguration implements WebMvcConfigurer {

    private final PlatformAuthorizationProperties properties;

    public AuthorizationWebMvcAutoConfiguration(PlatformAuthorizationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthorizationContextResolver(properties));
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationRequestContextFilter.class)
    public FilterRegistrationBean<AuthorizationRequestContextFilter> authorizationRequestContextFilter() {
        FilterRegistrationBean<AuthorizationRequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthorizationRequestContextFilter(properties));
        registration.setName("authorizationRequestContextFilter");
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
