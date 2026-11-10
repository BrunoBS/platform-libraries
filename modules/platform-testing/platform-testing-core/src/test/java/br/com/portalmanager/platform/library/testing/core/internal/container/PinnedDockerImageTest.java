package br.com.portalmanager.platform.library.testing.core.internal.container;
import br.com.portalmanager.platform.library.testing.core.internal.container.PinnedDockerImage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PinnedDockerImageTest {

    @Test
    void shouldParseVersionedImageAndDigestReferences() {
        assertThat(PinnedDockerImage.validatePinnedImage("registry.example:5000/kafka:3.8.1"))
                .isEqualTo("registry.example:5000/kafka:3.8.1");
        assertThat(PinnedDockerImage.validatePinnedImage(
                "registry.example/kafka@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .toString())
                .contains("@sha256:");
    }

    @Test
    void shouldRejectFloatingOrUnversionedImageReferences() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PinnedDockerImage.validatePinnedImage("localstack/localstack"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PinnedDockerImage.validatePinnedImage("localstack/localstack:latest"));
    }
}
