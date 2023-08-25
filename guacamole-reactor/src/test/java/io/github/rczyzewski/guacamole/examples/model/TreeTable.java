package io.github.rczyzewski.guacamole.examples.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBDocument;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;

import java.util.List;

@DynamoDBTable
@Value
@Builder
@With
public class TreeTable {

  @DynamoDBHashKey String uid;

  TreeBranch content;

  @With
  @Value
  @Builder
  @DynamoDBDocument
  public static class TreeBranch {
    @Singular List<TreeBranch> subbranches;
    String payload;
  }
}
