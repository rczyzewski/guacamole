package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;


@Value
@Builder
@DynamoDBTable
@With
public class Books {
  @DynamoDBHashKey String isbn;
}
