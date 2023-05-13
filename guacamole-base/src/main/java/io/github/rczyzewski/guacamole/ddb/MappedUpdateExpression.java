package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.UpdateExpression;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class MappedUpdateExpression<T, G extends ExpressionGenerator<T, G>>
{

    G generator;
    String tableName;
    Map<String, AttributeValue> keys;
    LogicalExpression<T> condition;
    List<UpdateExpression.SetExpression> setExpressions;

    public UpdateItemRequest serialize()
    {
        ConsecutiveIdGenerator idGenerator = ConsecutiveIdGenerator.builder().base("ABCDE").build();
        Optional<LogicalExpression<T>> preparedConditionExpression = Optional.ofNullable(this.condition)
                .map(it -> it.prepare(idGenerator));

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

        Map<String, String> attributeNames = setExpressions
                .stream().collect(Collectors.toMap(it -> "#" + it.getFieldCode(),
                        UpdateExpression.SetExpression::getFieldDdbName));

        return UpdateItemRequest.builder()
                                .key(keys)
                                .expressionAttributeValues(allValues)
                                .updateExpression("SET " + updateExpression)
                                .expressionAttributeNames(attributeNames)
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
            this.setExpressions
        );
    }

}
