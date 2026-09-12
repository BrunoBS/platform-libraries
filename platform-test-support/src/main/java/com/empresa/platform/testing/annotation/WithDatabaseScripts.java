package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.database.DatabaseCleanupPhase;
import com.empresa.platform.testing.database.DatabaseScriptExtension;
import com.empresa.platform.testing.database.DatabaseSetupPhase;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(DatabaseScripts.class)
@ExtendWith(DatabaseScriptExtension.class)
public @interface WithDatabaseScripts {

    String[] setup() default {};

    String[] cleanup() default {};

    DatabaseSetupPhase setupPhase() default DatabaseSetupPhase.BEFORE_TEST_CLASS;

    DatabaseCleanupPhase cleanupPhase() default DatabaseCleanupPhase.AFTER_TEST_CLASS;

    boolean continueOnError() default false;
}
