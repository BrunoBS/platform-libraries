package br.com.portalmanager.platform.library.audit.annotation;

import br.com.portalmanager.platform.library.audit.model.AuditAction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the business fact captured by platform-audit for a use case. */
@Repeatable(Auditables.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    AuditAction action();

    String event();

    String resourceType();
}
