package br.com.portalmanager.platform.library.testing.annotation;

import br.com.portalmanager.platform.library.testing.unit.PlatformUnitTestExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(MockitoExtension.class)
@ExtendWith(PlatformUnitTestExtension.class)
public @interface PlatformUnitTest {
}
