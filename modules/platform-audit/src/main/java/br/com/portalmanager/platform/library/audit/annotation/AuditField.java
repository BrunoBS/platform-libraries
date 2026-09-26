package br.com.portalmanager.platform.library.audit.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AuditField {
    AuditFieldSource source() default AuditFieldSource.PATH;
    String field() default "";
}
