package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@AllArgsConstructor
public class MappedScanExpression<T, G extends ExpressionGenerator<T>>{
    private final G generator;
    private final String tableName;
    private final LogicalExpression<T> condition;
    private final LiveMappingDescription<T> liveMappingDescription;

    public MappedScanExpression<T, G> withCondition(Function<G, LogicalExpression<T>> condition)
    {

        return this.condition != null ? this : new MappedScanExpression<>(
                generator,
                this.tableName,
                condition.apply(generator),
                liveMappingDescription
        );
    }
    public ScanRequest asScanItemRequest(){

       final ConsecutiveIdGenerator idGenerator = ConsecutiveIdGenerator.builder().base("ABCDE").build();
        Map<String, String>  shortCodeAccumulator = new HashMap<>();
        this.liveMappingDescription.getDict().forEach((k, v )-> shortCodeAccumulator.put(k,"#" + v.getShortCode()));

        Optional<LogicalExpression<T>> preparedConditionExpression =
                Optional.ofNullable(this.condition)
                        .map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator));

        Map<String, AttributeValue> allValues =
                preparedConditionExpression.map(LogicalExpression::getValuesMap)
                        .filter(it->!it.isEmpty())
                        .orElse(null);

        Map<String, String> allAttributeNames = preparedConditionExpression
                .map(LogicalExpression::getAttributesMap)
                .orElse(Collections.emptyMap());


        return ScanRequest.builder()
                //.key(keys)
                .expressionAttributeValues(allValues)
                .expressionAttributeNames(allAttributeNames)
                .select(Select.ALL_ATTRIBUTES)
                .filterExpression(preparedConditionExpression.map(LogicalExpression::serialize)
                        .orElse(null))
                .tableName(tableName)
                .build();
    }
}
