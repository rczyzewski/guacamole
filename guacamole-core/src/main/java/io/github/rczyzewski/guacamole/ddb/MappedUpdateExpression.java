package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.UpdateExpression;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@AllArgsConstructor
public class MappedUpdateExpression<T, G extends ExpressionGenerator<T>>
{

    private final G generator;
    private final String tableName;
    private final Map<String, AttributeValue> keys;
    private final LogicalExpression<T> condition;
    private final List<UpdateExpression.SetExpression> setExpressions;
    private final LiveMappingDescription<T> liveMappingDescription;

    public UpdateItemRequest asUpdateItemRequest()
    {
        ConsecutiveIdGenerator idGenerator = ConsecutiveIdGenerator.builder().base("ABCDE").build();
        Map<String, String>  shortCodeAccumulator = new HashMap<>();
        this.liveMappingDescription.getDict().forEach((k, v )-> shortCodeAccumulator.put(k,"#" + v.getShortCode()));

        Optional<LogicalExpression<T>> preparedConditionExpression =
                Optional.ofNullable(this.condition)
                        .map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator));


        Map<String, AttributeValue> values = setExpressions
            .stream()
            .map(UpdateExpression.SetExpression::getValue)
            .map(UpdateExpression.ConstantValue::getValues)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, AttributeValue> allValues = Stream.of(values,
                        preparedConditionExpression.map(LogicalExpression::getValuesMap).orElse(Collections.emptyMap()))
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String updateExpression = setExpressions.stream()
                .map(it -> String.format(" #%s = %s", it.getFieldCode(),
                        it.getValue().serialize()))
                .collect(Collectors.joining(","));

        Map<String, String> attributeNames =
                setExpressions
                .stream().collect(Collectors.toMap(it -> "#" + it.getFieldCode(),
                        UpdateExpression.SetExpression::getFieldDdbName));

        Map<String, String> attributesFromConditions = preparedConditionExpression
                .map(LogicalExpression::getAttributesMap)
                .orElse(Collections.emptyMap());

        Map<String, String> allAttributesName =
                new HashMap<>(attributeNames);
        allAttributesName.putAll(attributesFromConditions);


        return UpdateItemRequest.builder()
                                .key(keys)
                                .expressionAttributeValues(allValues)
                                .updateExpression("SET " + updateExpression)
                                .expressionAttributeNames(allAttributesName)
                                .conditionExpression(preparedConditionExpression.map(LogicalExpression::serialize)
                                                                                .orElse(null))
                                .tableName(tableName)
                                .build();
    }

    public MappedUpdateExpression<T, G> withCondition(Function<G, LogicalExpression<T>> condition)
    {

        return this.condition != null ? this : new MappedUpdateExpression<>(
            generator,
            this.tableName, this.keys,
            condition.apply(generator),
            this.setExpressions,
            liveMappingDescription
        );
    }

}
