package br.com.portalmanager.platform.library.audit.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Maps an audit metadata key to a top-level attribute in the returned resource snapshot. */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditAttribute {
    String key();
    String value();
}
