package br.com.portalmanager.platform.library.audit.recovery;

import java.util.Optional;

public interface AuditRecoveryLock {

    Optional<String> tryAcquire();

    void release(String token);
}
