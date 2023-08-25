package io.github.rczyzewski.guacamole.examples.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBIndexHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBIndexRangeKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
@DynamoDBTable
public class GlobalRangeIndexTable {

  @DynamoDBHashKey String uid;

  @DynamoDBIndexHashKey(globalSecondaryIndexNames = {"globalSecondaryIndexName"})
  String globalId;

  @DynamoDBIndexRangeKey(globalSecondaryIndexNames = {"globalSecondaryIndexName"})
  String globalRange;

  String payload;
}
