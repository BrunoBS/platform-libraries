package br.com.portalmanager.platform.library.testing.container;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PlatformTestingContainerImagesTest {

    @Test
    void shouldParseVersionedImageAndDigestReferences() {
        assertThat(PlatformTestingContainerImages.parse("registry.example:5000/kafka:3.8.1").toString())
                .isEqualTo("registry.example:5000/kafka:3.8.1");
        assertThat(PlatformTestingContainerImages.parse(
                "registry.example/kafka@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .toString())
                .contains("@sha256:");
    }

    @Test
    void shouldRejectFloatingOrUnversionedImageReferences() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlatformTestingContainerImages.parse("localstack/localstack"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlatformTestingContainerImages.parse("localstack/localstack:latest"));
    }
}
