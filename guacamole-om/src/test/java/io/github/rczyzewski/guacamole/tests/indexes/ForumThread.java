package io.github.rczyzewski.guacamole.tests.indexes;

import io.github.rczyzewski.guacamole.ddb.datamodeling.*;
import lombok.Builder;
import lombok.Value;
import lombok.With;

/** based on: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LSI.html */
@Value
@Builder
@DynamoDBTable
@With
public class ForumThread {

  @DynamoDBHashKey
  @DynamoDBAttribute(attributeName = "ForumName")
  String forumName;

  @DynamoDBRangeKey
  @DynamoDBAttribute(attributeName = "Subject")
  String subject;

  @DynamoDBLocalIndexRangeKey(localSecondaryIndexName = "LastPostIndex")
  @DynamoDBAttribute(attributeName = "LastPostDateTime")
  String lastPostDateTime;

  Integer replies;
}
