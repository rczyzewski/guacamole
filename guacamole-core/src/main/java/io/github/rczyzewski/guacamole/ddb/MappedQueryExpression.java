package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import lombok.AllArgsConstructor;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.rczyzewski.guacamole.ddb.MappedExpressionUtils.prepare;

@AllArgsConstructor
public class MappedQueryExpression<T, G> {
    private final G generator;
    private final String index;

    @With
    private final String tableName;
    @With
    private final LogicalExpression<T> condition;

    @With
    private final LiveMappingDescription<T> liveMappingDescription;
    private final LogicalExpression<T> keyCondition;

    public MappedQueryExpression<T, G> condition(Function<G, LogicalExpression<T>> condition) {
        LogicalExpression<T> a = condition.apply(this.generator);
        return this.withCondition(a);
    }

    public QueryRequest asQuerytemRequest() {

        ConsecutiveIdGenerator cid = ConsecutiveIdGenerator.builder().build();

        Map<String, String> shortCodeAccumulator = new HashMap<>();
        Optional<MappedExpressionUtils.ResolvedExpression<T>> preparedConditionExpression =
                prepare(liveMappingDescription, condition, cid, shortCodeAccumulator);


        LogicalExpression<T> preparedKeyCondition = keyCondition.prepare(cid, liveMappingDescription, shortCodeAccumulator);

        Map<String, String> attributesFromFilteringCondition = preparedConditionExpression
                .map(MappedExpressionUtils.ResolvedExpression::getAttributes)
                .orElse(Collections.emptyMap());
        Map<String, String> allAttributesNames = Stream.of(
                        attributesFromFilteringCondition,
                        preparedKeyCondition.getAttributesMap())
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));


        Map<String, AttributeValue> valuesFilterCondition = preparedConditionExpression
                .map(MappedExpressionUtils.ResolvedExpression::getValues)
                .orElse(Collections.emptyMap());

        Map<String, AttributeValue> allAttributesValues = Stream.of(
                        valuesFilterCondition,
                        preparedKeyCondition.getValuesMap())
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        return QueryRequest
                .builder()
                .expressionAttributeValues(allAttributesValues)
                .indexName(index)
                .keyConditionExpression(preparedKeyCondition.serialize())
                .expressionAttributeNames(allAttributesNames)
                .select(Select.ALL_ATTRIBUTES)
                .filterExpression(preparedConditionExpression
                        .map(MappedExpressionUtils.ResolvedExpression::getExpression)
                        .map(LogicalExpression::serialize)
                        .orElse(null))
                .tableName(tableName)
                .build();
    }
}
