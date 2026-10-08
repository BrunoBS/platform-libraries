package br.com.portalmanager.platform.library.testing.database;

import br.com.portalmanager.platform.library.testing.container.PlatformTestingContainerImages;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.MySQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestConfiguration {

    static final String IMAGE_PROPERTY = "platform.testing.mysql.image";

    @Bean(destroyMethod = "stop")
    @ServiceConnection
    MySQLContainer<?> mySqlContainer(Environment environment) {
        String image = environment.getProperty(
                IMAGE_PROPERTY,
                PlatformTestingContainerImages.MYSQL);
        return new MySQLContainer<>(PlatformTestingContainerImages.parse(image))
                .withDatabaseName("integration_test")
                .withUsername("test")
                .withPassword("test");
    }
}
