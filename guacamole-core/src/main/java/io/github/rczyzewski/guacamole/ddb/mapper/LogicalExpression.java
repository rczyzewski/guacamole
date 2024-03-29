package io.github.rczyzewski.guacamole.ddb.mapper;

import io.github.rczyzewski.guacamole.ddb.path.Path;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * condition-expression ::= operand comparator operand | operand BETWEEN operand AND operand |
 * operand IN ( operand (',' operand (, ...) )) | function | condition AND condition | condition OR
 * condition | NOT condition | ( condition )
 *
 * <p>function ::= attribute_exists (path) | attribute_not_exists (path) | attribute_type (path,
 * type) | begins_with (path, substr) | contains (path, operand) | size (path)
 */
public interface LogicalExpression<T> {
  String serialize();

  LogicalExpression<T> prepare(
      ConsecutiveIdGenerator idGenerator,
      LiveMappingDescription<T> liveMappingDescription,
      Map<String, String> shortCodeAccumulator);

  Map<String, AttributeValue> getValuesMap();

  Map<String, String> getAttributesMap();

  @AllArgsConstructor
  @RequiredArgsConstructor
  class AttributeExists<K> implements LogicalExpression<K> {

    final boolean shouldExists;
    final Path<K> path;
    @With Map<String, String> shortCodeAccumulator;

    @Override
    public String serialize() {
      String serializedPath = path.serializeAsPartExpression(this.shortCodeAccumulator);
      if (shouldExists) {
        return String.format("attribute_exists(%s)", serializedPath);
      }
      return String.format("attribute_not_exists(%s)", serializedPath);
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {
      path.getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));

      return this.withShortCodeAccumulator(shortCodeAccumulator);
    }

    @Override
    public Map<String, AttributeValue> getValuesMap() {
      return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getAttributesMap() {
      Set<String> parts = path.getPartsName();
      return this.shortCodeAccumulator.entrySet().stream()
          .filter(it -> parts.contains(it.getKey()))
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
  }

  @RequiredArgsConstructor
  @AllArgsConstructor
  class AttributeType<K> implements LogicalExpression<K> {

    final Path<K> path;
    final ExpressionGenerator.AttributeType type;

    @With Map<String, String> shortCodeAccumulator;
    @With String valueCode;

    @Override
    public String serialize() {
      String serializedPath = path.serializeAsPartExpression(this.shortCodeAccumulator);
      return String.format("attribute_type( %s , %s )", serializedPath, valueCode);
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {
      path.getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));

      return this.withValueCode(":" + idGenerator.get())
          .withShortCodeAccumulator(shortCodeAccumulator);
    }

    @Override
    public Map<String, AttributeValue> getValuesMap() {
      return Collections.singletonMap(this.valueCode, AttributeValue.fromS(type.getType()));
    }

    @Override
    public Map<String, String> getAttributesMap() {
      Set<String> parts = path.getPartsName();
      return this.shortCodeAccumulator.entrySet().stream()
          .filter(it -> parts.contains(it.getKey()))
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
  }

  /*
   *   comparator ::=
   *       =
   *       | <>
   *       | <
   *       | <=
   *       | >
   *       | >=
   *
   */
  @AllArgsConstructor
  @Getter
  enum ComparisonOperator {
    EQUAL("="),
    NOT_EQUAL("<>"),
    LESS("<"),
    LESS_OR_EQUAL("<="),
    GREATER(">"),
    GREATER_OR_EQUAL(">="),
    BETWEEN("BETWEEN"),
    BEGINS_WITH("BEGINS");

    private final String symbol;
  }

  @Builder
  @RequiredArgsConstructor
  @AllArgsConstructor
  class ComparisonToReference<K> implements LogicalExpression<K> {
    final Path<K> path;
    final ComparisonOperator operator;
    final Path<K> otherPath;

    @With Map<String, String> shortCodeAccumulator;

    @Override
    public String serialize() {

      String path1 = path.serializeAsPartExpression(this.shortCodeAccumulator);
      String path2 = otherPath.serializeAsPartExpression(this.shortCodeAccumulator);
      return String.format(" %s %s %s", path1, operator.getSymbol(), path2);
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {
      path.getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));
      otherPath
          .getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));

      return this.withShortCodeAccumulator(shortCodeAccumulator);
    }

    @Override
    public Map<String, AttributeValue> getValuesMap() {
      return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getAttributesMap() {
      Set<String> parts = new HashSet<>();
      parts.addAll(path.getPartsName());
      parts.addAll(otherPath.getPartsName());

      return this.shortCodeAccumulator.entrySet().stream()
          .filter(it -> parts.contains(it.getKey()))
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
  }

  @Builder
  @AllArgsConstructor
  @RequiredArgsConstructor
  class Between<K> implements LogicalExpression<K> {
    final Path<K> path;
    final AttributeValue dynamoDBEncodedValue;
    final AttributeValue dynamoDBEncodedValue2;
    @With Map<String, String> shortCodeAccumulator;

    @With String shortValueCode;
    @With String shortValueCode2;

    @Override
    public String serialize() {
      String serializedPath = path.serializeAsPartExpression(this.shortCodeAccumulator);
      return String.format(
          " %s BETWEEN %s AND %s", serializedPath, shortValueCode, shortValueCode2);
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {

      path.getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));
      return this.withShortCodeAccumulator(shortCodeAccumulator)
          .withShortValueCode2(":" + idGenerator.get())
          .withShortValueCode(":" + idGenerator.get());
    }

    @Override
    public Map<String, AttributeValue> getValuesMap() {
      HashMap<String, AttributeValue> ret = new HashMap<>();
      ret.put(shortValueCode, dynamoDBEncodedValue);
      ret.put(shortValueCode2, dynamoDBEncodedValue2);
      return ret;
    }

    @Override
    public Map<String, String> getAttributesMap() {
      Set<String> parts = path.getPartsName();
      return this.shortCodeAccumulator.entrySet().stream()
          .filter(it -> parts.contains(it.getKey()))
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
  }

  @Builder
  @AllArgsConstructor
  @RequiredArgsConstructor
  class BeginsWith<K> implements LogicalExpression<K> {
    final Path<K> path;
    final AttributeValue dynamoDBEncodedValue;
    @With Map<String, String> shortCodeAccumulator;

    @With String shortValueCode;

    @Override
    public String serialize() {
      String serializedPath = path.serializeAsPartExpression(this.shortCodeAccumulator);
      return String.format(" begins_with( %s,  %s)", serializedPath, shortValueCode);
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {

      path.getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));
      return this.withShortCodeAccumulator(shortCodeAccumulator)
          .withShortValueCode(":" + idGenerator.get());
    }

    @Override
    public Map<String, AttributeValue> getValuesMap() {
      return Collections.singletonMap(shortValueCode, dynamoDBEncodedValue);
    }

    @Override
    public Map<String, String> getAttributesMap() {
      Set<String> parts = path.getPartsName();
      return this.shortCodeAccumulator.entrySet().stream()
          .filter(it -> parts.contains(it.getKey()))
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
  }

  @Builder
  @AllArgsConstructor
  @RequiredArgsConstructor
  class ComparisonToValue<K> implements LogicalExpression<K> {
    final Path<K> path;
    final ComparisonOperator operator;
    final AttributeValue dynamoDBEncodedValue;
    @With Map<String, String> shortCodeAccumulator;

    @With String shortValueCode;

    @Override
    public String serialize() {
      String serializedPath = path.serializeAsPartExpression(this.shortCodeAccumulator);
      return String.format(" %s %s %s", serializedPath, operator.getSymbol(), shortValueCode);
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {

      path.getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));
      return this.withShortCodeAccumulator(shortCodeAccumulator)
          .withShortValueCode(":" + idGenerator.get());
    }

    @Override
    public Map<String, AttributeValue> getValuesMap() {
      return Collections.singletonMap(shortValueCode, dynamoDBEncodedValue);
    }

    @Override
    public Map<String, String> getAttributesMap() {
      Set<String> parts = path.getPartsName();
      return this.shortCodeAccumulator.entrySet().stream()
          .filter(it -> parts.contains(it.getKey()))
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
  }

  @AllArgsConstructor
  @With
  class OrExpression<K> implements LogicalExpression<K> {
    final List<LogicalExpression<K>> args;

    @Override
    public String serialize() {
      if (args.size() == 1) {
        return String.format("%s ", args.get(0).serialize());
      } else {
        return args.stream()
            .map(LogicalExpression::serialize)
            .map(it -> String.format(" %s ", it))
            .collect(Collectors.joining(" or ", "(", ")"));
      }
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {
      return this.withArgs(
          args.stream()
              .map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator))
              .collect(Collectors.toList()));
    }

    @Override
    public Map<String, AttributeValue> getValuesMap() {
      return args.stream()
          .map(LogicalExpression::getValuesMap)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, String> getAttributesMap() {
      return args.stream()
          .map(LogicalExpression::getAttributesMap)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (s, s2) -> s));
    }
  }

  @With
  @Builder
  class AndExpression<K> implements LogicalExpression<K> {
    final List<LogicalExpression<K>> args;

    @Override
    public String serialize() {
      if (args.size() == 1) {
        return String.format("%s ", args.get(0).serialize());
      } else {
        return args.stream()
            .map(LogicalExpression::serialize)
            .map(it -> String.format(" %s ", it))
            .collect(Collectors.joining(" and ", "(", ")"));
      }
    }

    public Map<String, AttributeValue> getValuesMap() {
      return args.stream()
          .map(LogicalExpression::getValuesMap)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, String> getAttributesMap() {
      return args.stream()
          .map(LogicalExpression::getAttributesMap)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (s, s2) -> s));
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {
      return this.withArgs(
          args.stream()
              .map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator))
              .collect(Collectors.toList()));
    }
  }

  @With
  @AllArgsConstructor(staticName = "build")
  class NotExpression<K> implements LogicalExpression<K> {
    final LogicalExpression<K> arg;

    @Override
    public String serialize() {
      return String.format("NOT %s", arg.serialize());
    }

    public Map<String, AttributeValue> getValuesMap() {
      return arg.getValuesMap();
    }

    @Override
    public Map<String, String> getAttributesMap() {
      return arg.getAttributesMap();
    }

    @Override
    public LogicalExpression<K> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<K> liveMappingDescription,
        Map<String, String> shortCodeAccumulator) {
      return this.withArg(arg.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator));
    }
  }
}
