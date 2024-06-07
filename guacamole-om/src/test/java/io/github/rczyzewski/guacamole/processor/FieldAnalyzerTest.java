package io.github.rczyzewski.guacamole.processor;

import java.nio.file.Paths;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class FieldAnalyzerTest {

  @Test
  void runningCompiler() {

    // based on: https://blog.frankel.ch/compilation-java-code-on-the-fly/
    JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
    log.info(
        "path for source code file {}",
        Paths.get("./src/main/test/java/SmallTable.java").toAbsolutePath());

    jc.run(
        System.in,
        System.out,
        System.err,
        Paths.get("./src/test/java/SmallTable.java").toAbsolutePath().toString());
  }
}
