package io.github.rczyzewski.guacamole.ddb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate mapper/path/expressions based on properties given in the class.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@Inherited
public @interface DynamoDBDocument {}
