package io.github.rczyzewski.guacamole.ddb.processor.model;

import io.github.rczyzewski.guacamole.ddb.processor.generator.NotSupportedTypeException;
import javax.lang.model.element.Element;
import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Getter
@AllArgsConstructor
public enum DDBType {
  STRING("s", String.class, true) {
  },
  INTEGER("n", Integer.class, false) {
  },
  DOUBLE("n", Double.class, false) {
  },
  FLOAT("n", Float.class, false) {
  },
  LONG("n", Long.class, false) {
  },
  NATIVE("UNKNOWN", AttributeValue.class, false),
  OTHER("UNKNOWN", NotSupportedTypeException.class, false) {
      @Override
      public boolean match(Element e) {
        return true;
      }
  };

  private final String symbol;
  private final Class<?> clazz;
  private final boolean isListQueryable;

  public  boolean match(Element e){
    return clazz.getCanonicalName().equals(e.asType().toString());
  }
}
