package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.UpdateExpression;
import io.github.rczyzewski.guacamole.ddb.path.Path;
import lombok.*;
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

import static io.github.rczyzewski.guacamole.ddb.MappedExpressionUtils.prepare;



@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class MappedUpdateExpression<T, G extends ExpressionGenerator<T>>
{

    private final G generator;
    private final String tableName;
    private final Map<String, AttributeValue> keys;
    @With
    private final LogicalExpression<T> condition;
    private final List<UpdateExpression.SetExpression> setExpressions;

    @Singular(value="set")
    private  Map<Path<T> , AttributeValue> extraSetExpressions;
    @Singular(value="add")
    private  Map<Path<T> , AttributeValue> addExpressions;
    @Singular(value="remove")
    private  List<Path<T>> remove;
    @Singular(value="delete")
    private  Map<Path<T>, UpdateExpression.AssignableValue> deleteExpressions;

    private final LiveMappingDescription<T> liveMappingDescription;
    public UpdateItemRequest asUpdateItemRequest()
    {
        Optional<MappedExpressionUtils.ResolvedExpression<T>> preparedConditionExpression =
                prepare(liveMappingDescription, condition);

        Map<String, AttributeValue> values = setExpressions
            .stream()
            .map(UpdateExpression.SetExpression::getValue)
            .map(UpdateExpression.ConstantValue::getValues)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, AttributeValue> allValues = Stream.of(values,
                        preparedConditionExpression
                                .map(MappedExpressionUtils.ResolvedExpression::getValues)
                                .orElse(Collections.emptyMap()))
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
                .map(MappedExpressionUtils.ResolvedExpression::getAttributes)
                .orElse(Collections.emptyMap());

        Map<String, String> allAttributesName =
                new HashMap<>(attributeNames);
        allAttributesName.putAll(attributesFromConditions);


        return UpdateItemRequest.builder()
                .key(keys)
                .expressionAttributeValues(allValues)
                .updateExpression("SET " + updateExpression)
                .expressionAttributeNames(allAttributesName)
                .conditionExpression(preparedConditionExpression.map(MappedExpressionUtils.ResolvedExpression::getExpression)
                        .map(LogicalExpression::serialize)
                        .orElse(null))
                .tableName(tableName)
                .build();
    }

    public MappedUpdateExpression<T, G> condition(Function<G, LogicalExpression<T>> condition)
    {
        return this.withCondition( condition.apply(generator));
    }

}
