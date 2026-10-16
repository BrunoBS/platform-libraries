package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.internal.RedisErrors;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class RedisCodecTest {
    @Test
    void protectsSizeAndTypeAndDoesNotTrustClassNames() {
        var codec = new RedisJsonCodec(new JsonMapper(), 32);
        assertThatThrownBy(() -> codec.encode("a".repeat(33)))
                .isInstanceOf(ApiException.class)
                .hasMessage("redis.limit");
        assertThatThrownBy(() -> codec.decode("1", String.class)).isInstanceOf(ApiException.class);
        assertThat(codec.decode("{\"@class\":\"evil.Class\"}", java.util.Map.class))
                .containsEntry("@class", "evil.Class");
    }

    @Test
    void sanitizesEntireTechnicalCause() {
        var failure =
                RedisErrors.translate(
                        new RuntimeException("WRONGPASS password=secret payload=private"));
        assertThat(failure.getMessageKey()).isEqualTo("redis.authentication");
        assertThat(failure.getCause().getMessage()).doesNotContain("password", "secret", "private");
        assertThat(failure.getCause().getCause()).isNull();
        assertThat(
                        RedisErrors.translate(
                                        new RuntimeException("NOPERM restricted test credential"))
                                .getMessageKey())
                .isEqualTo("redis.authentication");
    }
}
