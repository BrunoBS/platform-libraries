package br.com.portalmanager.platform.library.testing.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformArchitectureExtensionTest {

    @Test
    void packageMatchIncludesOnlyExactPackageAndSubpackages() {
        assertTrue(PlatformArchitectureExtension.packageMatches(
                "com.example.application",
                "com.example.application"
        ));
        assertTrue(PlatformArchitectureExtension.packageMatches(
                "com.example.application.service",
                "com.example.application"
        ));
        assertFalse(PlatformArchitectureExtension.packageMatches(
                "com.example.applicationTest",
                "com.example.application"
        ));
    }

    @Test
    void conventionCoverageUsesTheProductionPackageAndDoesNotCollideOnSimpleName() {
        assertTrue(PlatformArchitectureExtension.hasConventionBasedTest(
                "com.example.billing.PaymentService",
                Set.of("com.example.billing.PaymentServiceTest")
        ));
        assertFalse(PlatformArchitectureExtension.hasConventionBasedTest(
                "com.example.billing.PaymentService",
                Set.of("com.example.shipping.PaymentServiceTest")
        ));
    }

    @Test
    void supportsAllDocumentedTestClassSuffixes() {
        assertTrue(PlatformArchitectureExtension.hasConventionBasedTest(
                "com.example.billing.PaymentService",
                Set.of("com.example.billing.PaymentServiceIntegrationTest")
        ));
        assertTrue(PlatformArchitectureExtension.hasConventionBasedTest(
                "com.example.billing.PaymentService",
                Set.of("com.example.billing.PaymentServiceIT")
        ));
    }
}
