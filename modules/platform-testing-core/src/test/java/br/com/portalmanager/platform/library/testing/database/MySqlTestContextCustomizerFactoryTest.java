package br.com.portalmanager.platform.library.testing.database;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlTestContextCustomizerFactoryTest {

    private final MySqlTestContextCustomizerFactory factory = new MySqlTestContextCustomizerFactory();

    @Test
    void shouldUseDefaultImageWhenAnnotationDoesNotOverrideIt() {
        var customizer = factory.createContextCustomizer(DefaultImageTest.class, List.of());
        var context = new AnnotationConfigApplicationContext();

        customizer.customizeContext(context, null);

        assertThat(context.getEnvironment().getProperty(MySqlTestConfiguration.IMAGE_PROPERTY))
                .isEqualTo("mysql:8.4.11");
        context.close();
    }

    @Test
    void shouldPassConfiguredImageToSpringContext() {
        var customizer = factory.createContextCustomizer(OlderImageTest.class, List.of());
        var context = new AnnotationConfigApplicationContext();

        customizer.customizeContext(context, null);

        assertThat(context.getEnvironment().getProperty(MySqlTestConfiguration.IMAGE_PROPERTY))
                .isEqualTo("mysql:8.4.0");
        context.close();
    }

    @Test
    void shouldAllowPinnedMysqlNineImage() {
        var customizer = factory.createContextCustomizer(NineSeriesImageTest.class, List.of());
        var context = new AnnotationConfigApplicationContext();

        customizer.customizeContext(context, null);

        assertThat(context.getEnvironment().getProperty(MySqlTestConfiguration.IMAGE_PROPERTY))
                .isEqualTo("mysql:9.7.2");
        context.close();
    }

    @Test
    void shouldIncludeImageInContextCacheKey() {
        var defaultImage = factory.createContextCustomizer(DefaultImageTest.class, List.of());
        var olderImage = factory.createContextCustomizer(OlderImageTest.class, List.of());

        assertThat(defaultImage).isNotEqualTo(olderImage);
    }

    @Test
    void shouldRejectFloatingImageTagWithPlatformError() {
        assertThatThrownBy(() -> factory.createContextCustomizer(FloatingImageTest.class, List.of()))
                .isInstanceOf(PlatformConfigurationException.class)
                .satisfies(exception -> assertThat(
                        ((PlatformConfigurationException) exception).getErrorResponse().code())
                        .isEqualTo("PLT-TST-004"));
    }

    @WithMySql
    private static final class DefaultImageTest {
    }

    @WithMySql(image = "mysql:8.4.0")
    private static final class OlderImageTest {
    }

    @WithMySql(image = "mysql:latest")
    private static final class FloatingImageTest {
    }

    @WithMySql(image = "mysql:9.7.2")
    private static final class NineSeriesImageTest {
    }
}
