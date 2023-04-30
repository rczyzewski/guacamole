package io.github.rczyzewski.guacamole.processor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Paths;

@Slf4j
class FieldAnalyzerTest
{

    @Test
    void runningCompiler()
    {

        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        log.info("dddd {}", Paths.get("./src/main/test/java/SmallTable.java").toAbsolutePath());

        //https://blog.frankel.ch/compilation-java-code-on-the-fly/

        File f = new File(Paths.get("./src/main/test/java/SmallTable.java").toAbsolutePath().toString());

        jc.run(System.in, System.out, System.err, Paths.get("./src/test/java/SmallTable.java")
            .toAbsolutePath()
            .toString());
    }

}