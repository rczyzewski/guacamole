package io.github.rczyzewski.guacamole.examples.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBLocalIndexRangeKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBRangeKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
@DynamoDBTable
public class LocalSecondaryIndexTable {

  @DynamoDBHashKey String uid;

  @DynamoDBRangeKey String range;

  @DynamoDBLocalIndexRangeKey(localSecondaryIndexName = "secondRange")
  String secondRange;

  String payload;
}
