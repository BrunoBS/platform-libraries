package br.com.portalmanager.platform.library.testing.database;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DatabaseScriptExecutorTest {

    @Test
    void shouldFailFastWhenScriptDoesNotExist() {
        DataSource dataSource = mock(DataSource.class);

        assertThatThrownBy(() -> DatabaseScriptExecutor.execute(
                dataSource,
                new DefaultResourceLoader(),
                new String[]{"classpath:sql/missing-script.sql"},
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing-script.sql");
    }
}
