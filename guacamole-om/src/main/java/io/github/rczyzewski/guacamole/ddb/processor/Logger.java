package io.github.rczyzewski.guacamole.ddb.processor;

import javax.lang.model.element.Element;

public interface Logger {

  void info(String arg);

  void info(String arg, Element element);

  void warn(String arg);

  void warn(String arg, Element element);

  void error(String arg);

  void error(String arg, Element element);
}
