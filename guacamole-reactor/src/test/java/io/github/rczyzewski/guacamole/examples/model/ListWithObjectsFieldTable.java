package io.github.rczyzewski.guacamole.examples.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBDocument;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.List;

@Value
@Builder
@With
@DynamoDBTable
public class ListWithObjectsFieldTable {
    @DynamoDBHashKey
    String uid;

    List<InnerObject> payload;


    @With
    @Value
    @Builder
    @DynamoDBDocument
    public static class InnerObject{
        String name;
        String age;
    }
}


