package br.com.portalmanager.platform.library.audit.internal.aspect;

import br.com.portalmanager.platform.library.audit.api.annotation.Auditable;

/** A snapshot paired with the business fact that describes it. */
public record CapturedAuditSnapshot(Auditable auditable, Object value) {
}
