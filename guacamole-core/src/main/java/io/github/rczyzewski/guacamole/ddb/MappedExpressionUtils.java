package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import java.util.*;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@UtilityClass
public class MappedExpressionUtils {

  public static <T> Optional<ResolvedExpression<T>> prepare(
      LiveMappingDescription<T> liveMappingDescription,
      LogicalExpression<T> condition,
      ConsecutiveIdGenerator idGenerator,
      Map<String, String> shortCodeAccumulator) {

    liveMappingDescription
        .getDict()
        .forEach((k, v) -> shortCodeAccumulator.put(k, "#" + v.getShortCode()));

    return Optional.ofNullable(condition)
        .map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator))
        .map(
            it ->
                ResolvedExpression.<T>builder()
                    .expression(it)
                    .attributes(it.getAttributesMap())
                    .values(
                        Optional.of(it.getValuesMap()).filter(val -> !val.isEmpty()).orElse(null))
                    .build());
  }

  @Value
  @Builder
  public static class ResolvedExpression<T> {
    LogicalExpression<T> expression;
    Map<String, String> attributes;
    Map<String, AttributeValue> values;
  }
}
