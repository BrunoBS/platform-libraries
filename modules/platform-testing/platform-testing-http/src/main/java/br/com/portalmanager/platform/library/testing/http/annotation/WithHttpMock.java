package br.com.portalmanager.platform.library.testing.http.annotation;

import br.com.portalmanager.platform.library.testing.http.HttpMockExtension;
import br.com.portalmanager.platform.library.testing.http.HttpMockTestConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Starts and resets a context-scoped HTTP dependency simulator. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(HttpMockTestConfiguration.class)
@ExtendWith(HttpMockExtension.class)
@ResourceLock("platform-http-mock")
public @interface WithHttpMock {
}
