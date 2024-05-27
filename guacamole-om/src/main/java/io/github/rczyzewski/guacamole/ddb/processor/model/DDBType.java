package io.github.rczyzewski.guacamole.ddb.processor.model;

import io.github.rczyzewski.guacamole.ddb.mapper.StandardConverters;
import io.github.rczyzewski.guacamole.ddb.processor.generator.NotSupportedTypeException;
import javax.lang.model.element.Element;
import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Getter
@AllArgsConstructor
public enum DDBType {
  STRING("s", String.class, true, StandardConverters.StringConverter.class) {},
  INTEGER("n", Integer.class, false, StandardConverters.IntegerConverter.class) {},
  DOUBLE("n", Double.class, false, StandardConverters.DoubleConverter.class) {},
  FLOAT("n", Float.class, false, StandardConverters.FloatConverter.class) {},
  LONG("n", Long.class, false, StandardConverters.LongConverter.class) {},
  NATIVE("l", AttributeValue.class, false, StandardConverters.AttributeConverter.class),
  OTHER("UNKNOWN", NotSupportedTypeException.class, false, null) {
    @Override
    public boolean match(Element e) {
      return true;
    }
  };

  private final String symbol;
  private final Class<?> clazz;
  private final boolean isListQueryable;
  private final Class<?> standardConverterClass;

  public boolean match(Element e) {
    return clazz.getCanonicalName().equals(e.asType().toString());
  }
}
