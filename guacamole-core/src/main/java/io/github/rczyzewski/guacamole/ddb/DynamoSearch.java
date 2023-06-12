package io.github.rczyzewski.guacamole.ddb;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Condition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@With
@Value
@Builder
public class DynamoSearch
{
    String tableName;
    String indexName;
    @Builder.Default
    Map<String, Condition> keyConditions = Collections.emptyMap();
    List<String> attributeNames;
    String filterExpression;
    Map<String, AttributeValue> expressionAttributeValues;
    @Builder.Default
    Map<String, Condition> filterConditions = Collections.emptyMap();
}
