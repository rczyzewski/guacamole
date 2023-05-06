package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.UpdateExpression;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class MappedUpdateExpression<T>
{

    String tableName;
    Map<String, AttributeValue> keys;
    LogicalExpression<T> condition;
    List<UpdateExpression.SetExpression> setExpressions;

    public UpdateItemRequest serialize()
    {
        Map<String, AttributeValue> values = setExpressions
            .stream()
            .map(UpdateExpression.SetExpression::getValue)
            .map(UpdateExpression.ConstantValue::getValues)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var updateExpression = setExpressions.stream()
                                             .map(it -> String.format(" #%s = %s", it.getFieldCode(),
                                                                      it.getValue().serialize()))
                                             .collect(Collectors.joining(","));

        var attributeNames = setExpressions
            .stream().collect(Collectors.toMap(it -> "#" + it.getFieldCode(),
                                               UpdateExpression.SetExpression::getFieldDdbName));
        return UpdateItemRequest.builder()
                                .key(keys)
                                .expressionAttributeValues(values)
                                .updateExpression("SET " + updateExpression)
                                .expressionAttributeNames(attributeNames)
                                .tableName(tableName)
                                .build();
    }

    public MappedUpdateExpression<T> withCondition(LogicalExpression<T> condition)
    {
        return this.condition == condition ? this : new MappedUpdateExpression<T>(this.tableName, this.keys, condition,
                                                                                  this.setExpressions);
    }

}
