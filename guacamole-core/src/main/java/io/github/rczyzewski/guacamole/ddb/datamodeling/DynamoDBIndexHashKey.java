package io.github.rczyzewski.guacamole.ddb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a hashKey for an index.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDBIndexHashKey {
  String[] globalSecondaryIndexNames() default {};
}
