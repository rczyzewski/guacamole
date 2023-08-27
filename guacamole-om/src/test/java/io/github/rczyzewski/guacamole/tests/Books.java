package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


@Value
@Builder
@DynamoDBTable
@With
public class Books {
  @DynamoDBHashKey String isbn;
  AttributeValue customData;
}
