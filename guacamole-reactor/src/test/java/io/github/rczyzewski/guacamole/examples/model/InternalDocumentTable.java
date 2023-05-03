package io.github.rczyzewski.guacamole.examples.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBDocument;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Builder
@Value
@DynamoDBTable
public class InternalDocumentTable
{

    @DynamoDBHashKey
    String uid;

    String payload;

    InternalDocumentContent content;

    @With
    @Value
    @Builder
    @DynamoDBDocument
    public static class InternalDocumentContent
    {

        String payload;
    }
}

