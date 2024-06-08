package io.github.rczyzewski.guacamole.ddb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 *  Allows a custom encoding of property. The GuacamoleConverter is now a marking interface.
 *
 * <code>
 *   public static AttributeValue toValue(T ldt)
 *   public static T valueOf(AttributeValue attributeValue)
 * </code>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDBConverted {
  Class<? extends GuacamoleConverter> converter();
}
