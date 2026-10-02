package br.com.portalmanager.platform.library.schemavalidation.cache;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import com.networknt.schema.Schema;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CaffeineCompiledSchemaCacheTest {

    @Test
    void separatesCompiledSchemasByPublishedVersion() {
        var cache = new CaffeineCompiledSchemaCache(Duration.ofMinutes(15), 500);
        var versionOne = new ResourceSchema("APPLICATION", "application", 1, "{}");
        var versionTwo = new ResourceSchema("APPLICATION", "application", 2, "{}");
        Schema compiled = mock(Schema.class);

        cache.put(versionOne, compiled);

        assertThat(cache.get(versionOne)).contains(compiled);
        assertThat(cache.get(versionTwo)).isEmpty();
    }

    @Test
    void expiresUnusedCompiledSchema() throws Exception {
        var cache = new CaffeineCompiledSchemaCache(Duration.ofMillis(20), 500);
        var resource = new ResourceSchema("MENU", "menu", 1, "{}");
        cache.put(resource, mock(Schema.class));

        Thread.sleep(60);

        assertThat(cache.get(resource)).isEmpty();
    }
}
