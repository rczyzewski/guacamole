package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class LiveMappingDescription<T>
{

    private final Supplier<T> supplier;
    private final List<FieldMappingDescription<T>> fields;
    private final Map<String, FieldMappingDescription<T>> dict;
    public LiveMappingDescription(Supplier<T> supplier, List<FieldMappingDescription<T>> fields){
        this.supplier = supplier;
        this.fields = fields;
        dict = fields.stream()
                .collect(Collectors.toMap(FieldMappingDescription::getDdbName, Function.identity()));


    }

    public T transform(Map<String, AttributeValue> m)
    {

         T initialObject  = supplier.get();


        for( Map.Entry<String,AttributeValue> e : m.entrySet() ){

            String key = e.getKey();
            AttributeValue value = e.getValue();

            if ( ! dict.containsKey(key)) continue;

            initialObject = dict.get(key)
                    .getWither()
                    .apply(initialObject,  value);
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


