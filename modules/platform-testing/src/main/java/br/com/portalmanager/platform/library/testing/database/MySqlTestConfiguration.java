package br.com.portalmanager.platform.library.testing.database;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import br.com.portalmanager.platform.library.testing.container.PlatformTestingContainerImages;
import org.testcontainers.containers.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestConfiguration {

    @Bean(destroyMethod = "stop")
    @ServiceConnection
    MySQLContainer<?> mySqlContainer() {
        return new MySQLContainer<>(PlatformTestingContainerImages.parse(PlatformTestingContainerImages.MYSQL))
                .withDatabaseName("integration_test")
                .withUsername("test")
                .withPassword("test");
    }
}
