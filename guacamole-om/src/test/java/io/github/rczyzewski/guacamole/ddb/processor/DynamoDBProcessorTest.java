package io.github.rczyzewski.guacamole.ddb.processor;

import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamoDBProcessorTest{


    @Test
    void ddd(){
        ClassDescription.builder()
                        .name("SomeClass")
                .build();


    }
}