package br.com.portalmanager.platform.testing.annotation;

import br.com.portalmanager.platform.testing.database.DatabaseScriptExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(DatabaseScriptExtension.class)
public @interface DatabaseScripts {

    WithDatabaseScripts[] value();
}
