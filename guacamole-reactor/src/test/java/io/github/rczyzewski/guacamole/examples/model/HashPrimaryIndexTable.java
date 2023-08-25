package io.github.rczyzewski.guacamole.examples.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@DynamoDBTable
@Value
@Builder
@With
public class HashPrimaryIndexTable {

  @DynamoDBHashKey String uid;

  String payload;
}
