/**
 * Example below is inspired/is implementation of:
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html
 */
package io.github.rczyzewski.guacamole.tests.indexes;

import io.github.rczyzewski.guacamole.ddb.datamodeling.*;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@DynamoDBTable
@With
public class PlayerRanking {

  @DynamoDBHashKey String userId;

  @DynamoDBIndexHashKey(globalSecondaryIndexNames = {"GameTitleIndex"})
  @DynamoDBRangeKey
  String gameTitle;

  @DynamoDBIndexRangeKey(globalSecondaryIndexNames = {"GameTitleIndex"})
  @DynamoDBAttribute(attributeName = "TopScore")
  Integer topScore;

  String topScoreDateTime;
  Integer wins;
  Integer loses;
}
