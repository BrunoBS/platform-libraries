package br.com.portalmanager.core.audit.recovery;

import java.util.Optional;

public interface AuditRecoveryLock {

    Optional<String> tryAcquire();

    void release(String token);
}
