package io.github.rczyzewski.guacamole.ddb.processor;

import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public class CompileTimeLogger implements Logger {
  private Messager msg;

  @SneakyThrows
  private void msg(Diagnostic.Kind kind, String arg) {
    String date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    msg.printMessage(kind, format("%s %s", date, arg));
  }

  @Override
  public void info(String arg) {
    msg(WARNING, arg);
  }
  @Override
  public void info(String arg, Element element) {
    this.msg.printMessage(WARNING, arg, element);
  }

  @Override
  public void warn(String arg, Element element) {
   this.msg.printMessage(WARNING, arg, element);
  }

  @Override
  public void warn(String arg) {
    msg(WARNING, arg );
  }

  @Override
  public void error(String arg) {
    msg(ERROR, arg);
  }

  @Override
  public void error(String arg, Element element) {
    this.msg.printMessage(ERROR, arg, element);
  }
}
