package io.github.rczyzewski.guacamole.processor;

import io.github.rczyzewski.guacamole.ddb.processor.Logger;
import lombok.extern.slf4j.Slf4j;

import javax.lang.model.element.Element;

@Slf4j
public class NormalLogger implements Logger {

  @Override
  public void info(String arg) {
    log.info(arg);
  }

  @Override
  public void info(String arg, Element element) {
    log.info(arg);
  }

  @Override
  public void warn(String arg) {
    log.warn(arg);
  }

  @Override
  public void warn(String arg, Element element) {
    log.warn(arg);
  }

  @Override
  public void error(String arg) {
    log.error(arg);
  }
  @Override
  public void error(String arg, Element element) {
    log.error(arg);
  }
}
