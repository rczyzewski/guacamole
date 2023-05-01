package io.github.rczyzewski.guacamole.processor;

import io.github.rczyzewski.guacamole.ddb.processor.ClassUtils;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LiveDescriptionGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class LiveDescriptionGeneratorTest
{

    @Test
    void testCodeGeneration()
    {

        ClassDescription a = ClassDescription.builder()
            .fieldDescriptions(Collections.emptyList())
            .name("SomeClassName")
            .packageName("some.package.name")
            .build();

        LiveDescriptionGenerator generator = new LiveDescriptionGenerator(new NormalLogger());
        assertThat(generator.createMapper(a)).matches(it -> !it.isEmpty());

    }

    @Test
    void testSimpleTableStructure()
    {

        ClassDescription a = ClassDescription.builder()
            .fieldDescriptions(Collections.singletonList(FieldDescription.builder()
                                                             .isHashKey(true)
                                                             .ddbType(DDBType.N)
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
    void testTableStructure()
    {

        ClassDescription a = ClassDescription.builder()
            .fieldDescriptions(
                Arrays.asList(FieldDescription.builder()
                                  .isHashKey(true)
                                  .attribute("primaryKeyHash")
                                  .ddbType(DDBType.N)
                                  .build(),

                              FieldDescription.builder()
                                  .isHashKey(true)
                                  .ddbType(DDBType.D)
                                  .attribute("primaryRange")
                                  .build())
            )
            .name("SomeClassName")
            .packageName("some.package.name")
            .build();

        LiveDescriptionGenerator generator = new LiveDescriptionGenerator(new NormalLogger());
        assertThat(generator.createTableDefinition(new ClassUtils(a, new NormalLogger())))
                  .matches(it -> !it.isEmpty());

    }

}