package io.github.rczyzewski.guacamole.ddb.processor.generator;

import javax.lang.model.element.Element;
import lombok.Getter;

@Getter
public class NotSupportedTypeException extends RuntimeException {
  public NotSupportedTypeException(String msg, Element e) {
    this(msg);
    element = e;
  }

  public NotSupportedTypeException(String msg) {
    super(msg);
  }

  transient Element element;
}
