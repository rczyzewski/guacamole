package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import lombok.AllArgsConstructor;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static io.github.rczyzewski.guacamole.ddb.MappedExpressionUtils.prepare;

@AllArgsConstructor
public class MappedDeleteExpression<T, G extends ExpressionGenerator<T>>{
    private final G generator;
    private final String tableName;
    private final Map<String, AttributeValue> keys;
    @With
    private final LogicalExpression<T> condition;
    private final LiveMappingDescription<T> liveMappingDescription;

    public MappedDeleteExpression<T, G> condition(Function<G, LogicalExpression<T>> condition)
    {
        return this.withCondition(condition.apply(generator));
    }

    public DeleteItemRequest asDeleteItemRequest() {

        ConsecutiveIdGenerator cid = ConsecutiveIdGenerator.builder().build();
        Map<String, String> accumulator = new HashMap<>();
        Optional<MappedExpressionUtils.ResolvedExpression<T>> preparedConditionExpression =
                prepare(liveMappingDescription, condition, cid, accumulator);

        Map<String, AttributeValue> allValues =
                        preparedConditionExpression
                                .map(MappedExpressionUtils.ResolvedExpression::getValues)
                                .orElse(null);

        Map<String, String> allAttributeNames = preparedConditionExpression
                .map(MappedExpressionUtils.ResolvedExpression::getAttributes)
                .orElse(Collections.emptyMap());


        return DeleteItemRequest.builder()
                .key(keys)
                .expressionAttributeValues(allValues)
                .expressionAttributeNames(allAttributeNames)
                .conditionExpression(preparedConditionExpression
                        .map(MappedExpressionUtils.ResolvedExpression::getExpression)
                        .map(LogicalExpression::serialize)
                        .orElse(null))
                .tableName(tableName)
                .build();
    }
}
