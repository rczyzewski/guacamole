package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
public class ListSizeExpression<K> {

    final boolean shouldExists;
    final String fieldName;
    @With
    String fieldCode;

    public String serialize(){
        return String.format("size(%s)" , this.fieldCode);
    }

    public ListSizeExpression<K> prepare(ConsecutiveIdGenerator idGenerator,
                                        LiveMappingDescription<K> liveMappingDescription, Map<String,String> shortCodeAccumulator){

        String sk = liveMappingDescription.getDict().get(fieldName).getShortCode();
        return this.withFieldCode("#" + sk);
    }

    public Map<String, AttributeValue> getValuesMap(){
        return Collections.emptyMap();
    }

    public Map<String, String> getAttributesMap(){
        return Collections.singletonMap(fieldCode, fieldName);
    }
}
