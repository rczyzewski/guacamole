package io.github.rczyzewski.guacamole.ddb.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LiveDescriptionGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.processor.NormalLogger;
import io.github.rczyzewski.guacamole.tests.Books;
import io.github.rczyzewski.guacamole.tests.Country;
import io.github.rczyzewski.guacamole.tests.Employee;
import io.github.rczyzewski.guacamole.tests.indexes.ForumThread;
import io.github.rczyzewski.guacamole.tests.indexes.PlayerRanking;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class DynamoDBProcessorTest {

  @Test
  @SneakyThrows
  void sunnyDayTest() {
    NormalLogger logger = new NormalLogger();
    List<FieldDescription> fields = Collections.singletonList(
            FieldDescription.builder()
                    .isHashKey(true)
                    .typeArgument(FieldDescription.TypeArgument.builder().typeName("String").packageName("java.util")
                                          .build())
                    .attribute("ID")
                    .ddbType(DDBType.STRING)
                    .name("ABC")
                    .build());

    ClassDescription classDescription =
        ClassDescription.builder()
            .packageName("io.foo.bar")
            .sourandingClasses(Collections.emptyMap())
            .fieldDescriptions(fields)
            .name("SomeClass")
            .build();

    DynamoDBProcessor dynamoDBProcessor =
        DynamoDBProcessor.builder()
            .descriptionGenerator(new LiveDescriptionGenerator(logger))
            .logger(logger)
            .build();

    @Cleanup
    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
    @Cleanup
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(byteArray));

    dynamoDBProcessor.generateRepositoryCode(classDescription).writeTo(bw);
    bw.flush();
    bw.close();
    Assertions.assertThat(byteArray.toString()).isNotBlank();
}
  @SneakyThrows
  static String readContentBasedOnCanonicalName(String packageName, String className) {
    String baseDir = new File(".").getCanonicalPath() + "/src/test/java/";
    Path path = Paths.get(baseDir + packageName.replaceAll("\\.", "/") + "/" + className + ".java");
    byte[] b = Files.readAllBytes(path);
    return new String(b);
  }

  private static Stream<Arguments> customAddTestCases() {
    return Stream.of(Country.class, Employee.class, PlayerRanking.class, Books.class, ForumThread.class)
        .map(
            it ->
                Arguments.of(
                    it.getCanonicalName(),
                    readContentBasedOnCanonicalName(
                        it.getPackage().getName(), it.getSimpleName())));
  }

  @MethodSource("customAddTestCases")
  @ParameterizedTest(name = "{index}. {0}")
  @SneakyThrows
  void compilationOfAlreadyUsedExample(String classFullName, String code) {
    // https://github.com/google/compile-testing/issues/329
    // lombok is required to compile it

    JavaFileObject entity = JavaFileObjects.forSourceString(classFullName, code);

    Class<?> lombokAnnotationProcessor =
        getClass()
            .getClassLoader()
            .loadClass("lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
    Class<?> lombokClaimingProcessor =
        getClass()
            .getClassLoader()
            .loadClass("lombok.launch.AnnotationProcessorHider$ClaimingProcessor");

    Compilation compilation =
        javac()
            .withProcessors(
                (Processor) lombokAnnotationProcessor.getDeclaredConstructor().newInstance(),
                (Processor) lombokClaimingProcessor.getDeclaredConstructor().newInstance(),
                new DynamoDBProcessor())
            .compile(entity);

    assertThat(compilation).succeeded();
  }

  @Test
  @SneakyThrows
  void compilationOfAEntityWithoutPrimaryKey() {
    JavaFileObject entity = JavaFileObjects.forResource("NoIndexDefined.java");

    Class<?> lombokAnnotationProcessor =
        getClass()
            .getClassLoader()
            .loadClass("lombok.launch.AnnotationProcessorHider$AnnotationProcessor");
    Class<?> lombokClaimingProcessor =
        getClass()
            .getClassLoader()
            .loadClass("lombok.launch.AnnotationProcessorHider$ClaimingProcessor");

    Compilation compilation =
        javac()
            .withProcessors(
                (Processor) lombokAnnotationProcessor.getDeclaredConstructor().newInstance(),
                (Processor) lombokClaimingProcessor.getDeclaredConstructor().newInstance(),
                new DynamoDBProcessor())
            .compile(entity);

    assertThat(compilation).failed();

    assertThat(compilation)
        .hadErrorContaining(
            "'there is no HashKey defined for null NoIndexDefined' while processing the"
                + " class: 'NoIndexDefined'");
  }
}
