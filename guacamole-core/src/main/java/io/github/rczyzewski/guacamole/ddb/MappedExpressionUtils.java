package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class MappedExpressionUtils {


    public static <T> Optional<ResolvedExpression<T>> prepare(
            LiveMappingDescription<T> liveMappingDescription,
            LogicalExpression<T> condition){
        ConsecutiveIdGenerator idGenerator = ConsecutiveIdGenerator.builder().base("ABCDE").build();
        Map<String, String> shortCodeAccumulator = new HashMap<>();
        liveMappingDescription.getDict().forEach((k, v )-> shortCodeAccumulator.put(k,"#" + v.getShortCode()));


         return Optional.ofNullable(condition)
                .map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator))
                .map( it-> ResolvedExpression.<T>builder()
               .expression(it)
                        .attributes(it.getAttributesMap())
                        .values(Optional.of(it.getValuesMap()).filter(val -> ! val.isEmpty()).orElse(null))
                        .build());
    }

    @Value
    @Builder
    public static class ResolvedExpression<T>{
       LogicalExpression<T>  expression;
       Map<String, String> attributes;
       Map<String, AttributeValue> values;
    }

}
