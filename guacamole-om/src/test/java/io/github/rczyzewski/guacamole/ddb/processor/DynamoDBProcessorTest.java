package io.github.rczyzewski.guacamole.ddb.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.github.rczyzewski.guacamole.ddb.processor.generator.IndexSelectorGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LiveDescriptionGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LogicalExpressionBuilderGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.PathGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.processor.NormalLogger;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

@Slf4j
class DynamoDBProcessorTest {


    @Test
    @SneakyThrows
    void sunnyDayTest() {
        NormalLogger logger = new NormalLogger();
        List<FieldDescription> fields = new ArrayList<>();
        fields.add(FieldDescription.builder().isHashKey(true).attribute("ID").typeName("java.lang.String").ddbType(DDBType.STRING).name("ABC").build());

        ClassDescription classDescription = ClassDescription.builder()
                .packageName("io.foo.bar") //TODO: reporting when class is in the default package
                .sourandingClasses(Collections.emptyMap())
                .fieldDescriptions(fields)
                .name("SomeClass")
                .build();

        DynamoDBProcessor a = DynamoDBProcessor.builder()
                .descriptionGenerator(new LiveDescriptionGenerator(logger))
                .expressionBuilderGenerator(new LogicalExpressionBuilderGenerator())
                .pathGenerator(new PathGenerator())
                .indexSelectorGenerator(new IndexSelectorGenerator())
                .logger(logger)
                .build();
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(byteArray));
        a.generateRepositoryCode(classDescription).writeTo(bw);
        bw.close();
        log.info("code: {}", byteArray);


    }

    @Test
    @SneakyThrows
    void compilationHappyDay() throws ClassNotFoundException {
        //https://github.com/google/compile-testing/issues/329
        //lombok is required to compile it

        JavaFileObject entity = JavaFileObjects.forResource("io/github/rczyzewski/guacamole/tests/FakeCountry.java");


        Class<?> lombokAnnotationProcessor = getClass().getClassLoader().loadClass("lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
        Class<?> lombokClaimingProcessor = getClass().getClassLoader().loadClass("lombok.launch.AnnotationProcessorHider$ClaimingProcessor");

        Compilation compilation = javac()
                .withProcessors(
                        (Processor) lombokAnnotationProcessor.getDeclaredConstructor().newInstance(),
                        (Processor) lombokClaimingProcessor.getDeclaredConstructor().newInstance(),
                        new DynamoDBProcessor())
                .compile(entity);

        assertThat(compilation).succeeded();
          /*
          assertThat(compilation)
                           .hadErrorContaining("No types named HelloWorld!")
                           .inFile(helloWorld)
                            .onLine(23)
                            .atColumn(5); */
    }
}