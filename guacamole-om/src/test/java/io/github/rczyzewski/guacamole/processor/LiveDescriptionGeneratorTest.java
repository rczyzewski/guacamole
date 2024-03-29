package io.github.rczyzewski.guacamole.processor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rczyzewski.guacamole.ddb.processor.ClassUtils;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LiveDescriptionGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class LiveDescriptionGeneratorTest {

  @Test
  void testCodeGeneration() {

    ClassDescription a =
        ClassDescription.builder()
            .fieldDescriptions(Collections.emptyList())
            .name("SomeClassName")
            .packageName("some.package.name")
            .build();

    LiveDescriptionGenerator generator = new LiveDescriptionGenerator(new NormalLogger());
    assertThat(generator.createMapper(a)).matches(it -> !it.isEmpty());
  }

  @Test
  void testSimpleTableStructure() {

    ClassDescription a =
        ClassDescription.builder()
            .fieldDescriptions(
                Collections.singletonList(
                    FieldDescription.builder()
                        .isHashKey(true)
                        .ddbType(DDBType.INTEGER)
                        .attribute("primaryKeyHash")
                        .build()))
            .name("SomeClassName")
            .packageName("some.package.name")
            .build();

    LiveDescriptionGenerator generator = new LiveDescriptionGenerator(new NormalLogger());
    assertThat(generator.createTableDefinition(new ClassUtils(a, new NormalLogger())))
        .matches(it -> !it.isEmpty());
  }

  @Test
  void testTableStructure() {

    ClassDescription a =
        ClassDescription.builder()
            .fieldDescriptions(
                Arrays.asList(
                    FieldDescription.builder()
                        .isHashKey(true)
                        .attribute("primaryKeyHash")
                        .ddbType(DDBType.INTEGER)
                        .build(),
                    FieldDescription.builder()
                        .isHashKey(true)
                        .ddbType(DDBType.DOUBLE)
                        .attribute("primaryRange")
                        .build()))
            .name("SomeClassName")
            .packageName("some.package.name")
            .build();

    LiveDescriptionGenerator generator = new LiveDescriptionGenerator(new NormalLogger());
    assertThat(generator.createTableDefinition(new ClassUtils(a, new NormalLogger())))
        .matches(it -> !it.isEmpty());
  }
}
