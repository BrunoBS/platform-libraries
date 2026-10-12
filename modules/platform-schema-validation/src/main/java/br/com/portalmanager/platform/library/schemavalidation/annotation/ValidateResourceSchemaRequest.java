package br.com.portalmanager.platform.library.schemavalidation.annotation;

import br.com.portalmanager.platform.library.schemavalidation.normalization.SchemaPayloadNormalizer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates the received JSON before Spring MVC binds it to this request type. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateResourceSchemaRequest {
    String type();

    String code();

    /** Optional Spring bean type. The default keeps the received JSON unchanged. */
    Class<? extends SchemaPayloadNormalizer> normalizer() default SchemaPayloadNormalizer.class;
}
