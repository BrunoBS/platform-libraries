package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.extension.PlatformIntegrationExtension;
import com.empresa.platform.testing.client.PlatformHttpTestConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PlatformHttpTestConfiguration.class)
@ExtendWith(PlatformIntegrationExtension.class)
public @interface PlatformIntegrationTest {
}
