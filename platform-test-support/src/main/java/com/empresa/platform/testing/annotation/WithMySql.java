package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.database.CleanupMode;
import com.empresa.platform.testing.mysql.MySqlTestConfiguration;
import com.empresa.platform.testing.mysql.MySqlTestExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(MySqlTestConfiguration.class)
@ExtendWith(MySqlTestExtension.class)
public @interface WithMySql {

    CleanupMode cleanup() default CleanupMode.BEFORE_EACH;

    String[] excludeTables() default {"flyway_schema_history"};
}
