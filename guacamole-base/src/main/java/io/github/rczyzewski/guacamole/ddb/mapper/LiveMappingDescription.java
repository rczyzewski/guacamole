package io.github.rczyzewski.guacamole.ddb.mapper;

import io.github.rczyzewski.guacamole.ddb.MappedUpdateExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.UpdateExpression.SetExpression;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.rczyzewski.guacamole.ddb.mapper.UpdateExpression.*;

@Slf4j
public class LiveMappingDescription<T>
{

    private final Supplier<T> supplier;
    private final List<FieldMappingDescription<T>> fields;
    private final Map<String, FieldMappingDescription<T>> dict;

    public LiveMappingDescription(Supplier<T> supplier, List<FieldMappingDescription<T>> fields)
    {
        this.supplier = supplier;
        this.fields = fields;
        dict = fields.stream()
                     .collect(Collectors.toMap(FieldMappingDescription::getDdbName, Function.identity()));

    }

    public <G extends ExpressionGenerator<T,G>> MappedUpdateExpression<T,G> generateUpdateExpression (T object , G generator,  String table)
    {
        ConsecutiveIdGenerator a = ConsecutiveIdGenerator.builder().base("abcde").build();
        List<SetExpression> setExpressions = fields.stream()
                .filter(it -> !it.isKeyValue())
                .map(it -> {
                    ConstantValue v = ConstantValue.builder()
                            .valueCode(a.get())
                            .attributeValue(it.getExport().apply(object)
                                    .orElse(AttributeValue.fromNul(true)))
                            .build();
                    return SetExpression.builder()
                            .fieldCode(it.getShortCode())
                            .fieldDdbName(it.getDdbName())
                            .value(v).build();
                })
                .collect(Collectors.toList());
        Map<String, AttributeValue> keys = this.exportKeys(object);

        return new MappedUpdateExpression<>(generator, table, keys, null, setExpressions);
    }

    public T transform(Map<String, AttributeValue> m)
    {

        T initialObject = supplier.get();

        for (Map.Entry<String, AttributeValue> e : m.entrySet()) {

            String key = e.getKey();
            AttributeValue value = e.getValue();

            if (!dict.containsKey(key)) continue;

            initialObject = dict.get(key)
                                .getWither()
                                .apply(initialObject, value);
        }
        return initialObject;
    }

    public Map<String, AttributeValue> export(T object)
    {
        return fields.stream().collect(
                         Collectors.toMap(FieldMappingDescription::getDdbName, it -> it.getExport().apply(object)))
                     .entrySet()
                     .stream()
                     .filter(it -> it.getValue().isPresent())
                     .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().get()))

            ;

    }

    public Map<String, AttributeValue> exportKeys(T object)
    {
        return fields.stream()
                     .filter(FieldMappingDescription::isKeyValue)
                     .collect(
                         Collectors.toMap(FieldMappingDescription::getDdbName, it -> it.getExport().apply(object)))
                     .entrySet()
                     .stream()
                     .filter(it -> it.getValue().isPresent())
                     .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().get()));

    }

    public Map<String, AttributeValueUpdate> exportUpdate(T object)
    {
        return fields.stream()
                     .filter(it -> !it.isKeyValue())
                     .collect(
                         Collectors.toMap(FieldMappingDescription::getDdbName, it -> it.getExport().apply(object)))
                     .entrySet()
                     .stream()
                     .filter(it -> it.getValue().isPresent())
                     .collect(Collectors.toMap(Map.Entry::getKey,
                                               it -> AttributeValueUpdate.builder()
                                                                         .action(AttributeAction.PUT)
                                                                         .value(it.getValue().get())
                                                                         .build()));

    }

}


