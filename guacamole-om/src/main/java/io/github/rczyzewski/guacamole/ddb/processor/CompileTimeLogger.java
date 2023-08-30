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

  public void info(String arg) {
    // TODO: change to Note
    msg(WARNING, arg);
  }
  public void warn(String arg, Element element) {
   this.msg.printMessage(WARNING, arg, element);
  }

  public void warn(String arg) {
    msg(WARNING, arg );
  }

  public void error(String arg) {
    msg(ERROR, arg);
  }
  public void error(String arg, Element element) {
    this.msg.printMessage(ERROR, arg, element);
  }
}
