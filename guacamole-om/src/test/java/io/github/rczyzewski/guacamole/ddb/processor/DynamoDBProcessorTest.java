package io.github.rczyzewski.guacamole.ddb.processor;

import io.github.rczyzewski.guacamole.ddb.processor.generator.IndexSelectorGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LiveDescriptionGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LogicalExpressionBuilderGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.PathGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.processor.NormalLogger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
class DynamoDBProcessorTest{


    @Test
    @SneakyThrows
    void sunnyDayTest(){
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
        BufferedWriter bw =  new BufferedWriter(new OutputStreamWriter(byteArray));
        a.generateRepositoryCode(classDescription).writeTo(bw);
        bw.close();
       log.info("code: {}",  byteArray);


    }
}