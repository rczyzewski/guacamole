package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.LocalDateTimeConverter;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBConverted;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;


@Value
@Builder
@DynamoDBTable
@With
public class Books {
  @DynamoDBHashKey String isbn;
  AttributeValue customData;

  @DynamoDBConverted(converter = LocalDateTimeConverter.class)
  LocalDateTime customData2;
}
