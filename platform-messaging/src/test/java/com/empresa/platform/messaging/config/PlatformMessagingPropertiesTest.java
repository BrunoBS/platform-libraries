package com.empresa.platform.messaging.config;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class PlatformMessagingPropertiesTest {

    @Test
    void shouldInitializeWithDefaultValues() {
        PlatformMessagingProperties properties = new PlatformMessagingProperties();
        
        assertTrue(properties.isEnabled());
        assertEquals("pt-BR", properties.getDefaultLocale());
        assertEquals("vw_api_message", properties.getDatasource().getViewName());
        assertFalse(properties.getCache().isEnabled());
        assertEquals(Duration.ofHours(1), properties.getCache().getTtl());
    }
}
