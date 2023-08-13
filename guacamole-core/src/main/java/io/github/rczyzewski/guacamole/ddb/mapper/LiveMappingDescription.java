package io.github.rczyzewski.guacamole.ddb.mapper;

import io.github.rczyzewski.guacamole.ddb.MappedDeleteExpression;
import io.github.rczyzewski.guacamole.ddb.MappedScanExpression;
import io.github.rczyzewski.guacamole.ddb.MappedUpdateExpression;
import io.github.rczyzewski.guacamole.ddb.path.PrimitiveElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Slf4j
@Getter
public class LiveMappingDescription<T> {

    private final Supplier<T> supplier;
    private final List<FieldMappingDescription<T>> fields;
    private final Map<String, FieldMappingDescription<T>> dict;

    public LiveMappingDescription(Supplier<T> supplier, List<FieldMappingDescription<T>> fields) {
        this.supplier = supplier;
        this.fields = fields;
        dict = fields.stream()
                .collect(Collectors.toMap(FieldMappingDescription::getDdbName, Function.identity()));

    }

    public Map<String, Collection<WriteRequest>> writeRequest(String table, T[] objects) {

        Collection <WriteRequest> dda = Arrays.stream(objects).map(object ->

                WriteRequest.builder()
                        .putRequest(PutRequest.builder().item(
                                fields.stream()
                                        .filter(it -> it.getExport().apply(object).isPresent())
                                        .collect(Collectors.toMap(
                                                FieldMappingDescription::getDdbName,
                                                it -> it.getExport().apply(object).get()))
                        ).build()).build()).collect(Collectors.toList());

        return Collections.singletonMap(table, dda);

}


    public <G extends ExpressionGenerator<T>> MappedDeleteExpression<T, G> generateDeleteExpression(T object, G generator, String table) {
        Map<String, AttributeValue> keys = this.exportKeys(object);
        return new MappedDeleteExpression<>(generator, table, keys, null, this);
    }

    public <G extends ExpressionGenerator<T>> MappedScanExpression<T, G> generateScanExpression(G generator, String table) {
        return new MappedScanExpression<>(generator, table, null, this);
    }

    public <G extends ExpressionGenerator<T>> MappedUpdateExpression<T, G> generateUpdateExpression(T object, G generator, String table) {
        MappedUpdateExpression.RczSetExpressionGenerator<T> ddd = new MappedUpdateExpression.RczSetExpressionGenerator<>();

        List<MappedUpdateExpression.Statement<T>> setExpressions = fields.stream()
                .filter(it -> !it.isKeyValue())
                .filter(it -> it.getExport().apply(object).isPresent())
                .map(it -> MappedUpdateExpression.UpdateStatement.<T>builder()
                        //Argument G - is not important
                        .path(new MappedUpdateExpression.RczPathExpression<>(PrimitiveElement.<T, G>builder().selectedElement(it.getDdbName()).build()))
                        .override(true)
                        .value(ddd.just(it.getExport().apply(object).orElseThrow(RuntimeException::new)))
                        .build())
                .collect(Collectors.toList());

        Map<String, AttributeValue> keys = this.exportKeys(object);

        return MappedUpdateExpression.<T, G>builder()
                .tableName(table)
                .generator(generator)
                .keys(keys)
                .extraSetAddRemoveExpressions(setExpressions)
                .liveMappingDescription(this)
                .build();
    }

    public T transform(Map<String, AttributeValue> m) {

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

    public Map<String, AttributeValue> export(T object) {
        return fields.stream().collect(
                        Collectors.toMap(FieldMappingDescription::getDdbName, it -> it.getExport().apply(object)))
                .entrySet()
                .stream()
                .filter(it -> it.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().get()));
    }

    public Map<String, AttributeValue> exportKeys(T object) {
        return fields.stream()
                .filter(FieldMappingDescription::isKeyValue)
                .collect(
                        Collectors.toMap(FieldMappingDescription::getDdbName, it -> it.getExport().apply(object)))
                .entrySet()
                .stream()
                .filter(it -> it.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().get()));

    }
}


