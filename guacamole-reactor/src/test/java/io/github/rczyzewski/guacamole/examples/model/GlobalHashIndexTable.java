package io.github.rczyzewski.guacamole.examples.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBIndexHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
@DynamoDBTable
public class GlobalHashIndexTable {

  @DynamoDBHashKey String uid;

  @DynamoDBIndexHashKey(globalSecondaryIndexNames = {"globalSecondaryIndexName"})
  String globalId;

  String payload;
}
