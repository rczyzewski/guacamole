package io.github.rczyzewski.guacamole.ddb.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.github.rczyzewski.guacamole.tests.Country;
import io.github.rczyzewski.guacamole.tests.Employee;
import io.github.rczyzewski.guacamole.tests.indexes.PlayerRanking;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class DynamoDBProcessorTest {

  /*
  @Test
  void sunnyDayTest() {
    NormalLogger logger = new NormalLogger();
    List<FieldDescription> fields = new ArrayList<>();
    fields.add(
        FieldDescription.builder()
            .isHashKey(true)
            .attribute("ID")
            .typeName("java.lang.String")
            .ddbType(DDBType.STRING)
            .name("ABC")
            .build());

    ClassDescription classDescription =
        ClassDescription.builder()
            .packageName("io.foo.bar") // TODO: reporting when class is in the default package
            .sourandingClasses(Collections.emptyMap())
            .fieldDescriptions(fields)
            .name("SomeClass")
            .build();

    DynamoDBProcessor dynamoDBProcessor =
        DynamoDBProcessor.builder()
            .descriptionGenerator(new LiveDescriptionGenerator(logger))
            .logger(logger)
            .build();

    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(byteArray));
    dynamoDBProcessor.generateRepositoryCode(classDescription).writeTo(bw);
    bw.flush();
    Assertions.assertThat(byteArray.toString()).isNotBlank();
    bw.close();
  } */

  @SneakyThrows
  static String readContentBasedOnCanonicalName(String packageName, String className) {
    String baseDir = new File(".").getCanonicalPath() + "/src/test/java/";
    Path path = Paths.get(baseDir + packageName.replaceAll("\\.", "/") + "/" + className + ".java");
    byte[] b = Files.readAllBytes(path);
    return new String(b);
  }

  private static Stream<Arguments> customAddTestCases() {
    return Stream.of(Country.class, Employee.class, PlayerRanking.class)
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
            "'there is no HashKey defined for unnamed package NoIndexDefined' while processing the"
                + " class: 'NoIndexDefined'");
  }
}
