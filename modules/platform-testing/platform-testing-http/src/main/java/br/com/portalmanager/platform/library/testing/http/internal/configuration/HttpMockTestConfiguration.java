package br.com.portalmanager.platform.library.testing.http.internal.configuration;
import br.com.portalmanager.platform.library.testing.http.api.mock.HttpMockServer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class HttpMockTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    HttpMockServer httpMockServer() {
        return new HttpMockServer();
    }
}
