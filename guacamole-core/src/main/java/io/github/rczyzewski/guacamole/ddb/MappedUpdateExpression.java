package io.github.rczyzewski.guacamole.ddb;

import static io.github.rczyzewski.guacamole.ddb.MappedExpressionUtils.prepare;

import io.github.rczyzewski.guacamole.ddb.mapper.*;
import io.github.rczyzewski.guacamole.ddb.path.Path;
import io.github.rczyzewski.guacamole.ddb.path.TypedPath;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Builder(toBuilder = true)
@AllArgsConstructor
public class MappedUpdateExpression<T, G extends ExpressionGenerator<T>> {

  private final G generator;
  private final String tableName;
  private final Map<String, AttributeValue> keys;

  @With private final LogicalExpression<T> condition;

  @Builder.Default
  private final List<Statement<T>> extraSetAddRemoveExpressions = new ArrayList<>();

  public MappedUpdateExpression<T, G> remove(Path<T> path) {
    extraSetAddRemoveExpressions.add(new RemoveStatement<>(new RczPathExpression<>(path)));
    return this;
  }

  public MappedUpdateExpression<T, G> setIfEmpty(
      Path<T> path, Function<RczSetExpressionGenerator<T>, RczSimpleExpression<T>> expr) {

    RczSetExpressionGenerator<T> eg = new RczSetExpressionGenerator<>();

    extraSetAddRemoveExpressions.add(
        UpdateStatement.<T>builder()
            .path(new RczPathExpression<>(path))
            .override(false)
            .value(expr.apply(eg))
            .build());
    return this;
  }

  public MappedUpdateExpression<T, G> set(
      Path<T> path, Function<RczSetExpressionGenerator<T>, RczSetExpression<T>> expr) {
    RczSetExpressionGenerator<T> eg = new RczSetExpressionGenerator<>();

    extraSetAddRemoveExpressions.add(
        UpdateStatement.<T>builder()
            .path(new RczPathExpression<>(path))
            .override(true)
            .value(expr.apply(eg))
            .build());
    return this;
  }

  public MappedUpdateExpression<T, G> add(TypedPath<T, Number> path, Number number) {
    RczValueExpression<T> ddd = new RczValueExpression<>(AttributeValue.fromN(number.toString()));
    RczPathExpression<T> pathExpression = new RczPathExpression<>(path);
    extraSetAddRemoveExpressions.add(
        AddStatement.<T>builder().value(ddd).path(pathExpression).build());
    return this;
  }

  public interface Statement<T> {
    RczPathExpression<T> getPath();

    String serialize();

    Statement<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulator);

    Map<String, String> getAttributes();

    Map<String, AttributeValue> getValues();
  }

  @Builder
  @With
  @Getter
  public static final class RemoveStatement<T> implements Statement<T> {
    RczPathExpression<T> path;

    public RemoveStatement<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulator) {
      return this.withPath(
          path.prepare(
              idGenerator,
              liveMappingDescription,
              shortCodeAccumulator,
              shortCodeValueAccumulator));
    }

    public Map<String, String> getAttributes() {
      return path.getAttributes();
    }

    public Map<String, AttributeValue> getValues() {
      return Collections.emptyMap();
    }

    public String serialize() {
      return path.serialize();
    }
  }

  @Builder
  @With
  @Getter
  public static final class AddStatement<T> implements Statement<T> {
    RczPathExpression<T> path;
    RczValueExpression<T> value;

    public AddStatement<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulator) {
      return this.withPath(
              path.prepare(
                  idGenerator,
                  liveMappingDescription,
                  shortCodeAccumulator,
                  shortCodeValueAccumulator))
          .withValue(
              value.prepare(
                  idGenerator,
                  liveMappingDescription,
                  shortCodeAccumulator,
                  shortCodeValueAccumulator));
    }

    public Map<String, String> getAttributes() {
      return path.getAttributes();
    }

    public Map<String, AttributeValue> getValues() {
      return value.getValues();
    }

    public String serialize() {
      return path.serialize() + " " + value.serialize();
    }
  }

  @Builder
  @With
  @Getter
  public static final class UpdateStatement<T> implements Statement<T> {
    RczPathExpression<T> path;
    RczSetExpression<T> value;

    @Builder.Default boolean override = true;

    public String serialize() {

      if (override) {
        return path.serialize() + " = " + value.serialize();
      }
      String serializedPath = path.serialize();
      return String.format(
          " %s = if_not_exists( %s , %s )", serializedPath, serializedPath, value.serialize());
    }

    @Override
    public Statement<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulator) {
      return this.withPath(
              path.prepare(
                  idGenerator,
                  liveMappingDescription,
                  shortCodeAccumulator,
                  shortCodeValueAccumulator))
          .withValue(
              value.prepare(
                  idGenerator,
                  liveMappingDescription,
                  shortCodeAccumulator,
                  shortCodeValueAccumulator));
    }

    @Override
    public Map<String, String> getAttributes() {
      return Stream.of(path, value)
          .map(RczSetExpression::getAttributes)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (tmp1, tmp2) -> tmp1));
    }

    @Override
    public Map<String, AttributeValue> getValues() {
      return value.getValues();
    }
  }

  private final LiveMappingDescription<T> liveMappingDescription;

  public UpdateItemRequest asUpdateItemRequest() {
    ConsecutiveIdGenerator idGenerator = ConsecutiveIdGenerator.builder().base("ABCDEFGH").build();

    Map<String, String> shortCodeAccumulator = new HashMap<>();
    Map<String, AttributeValue> shortCodeValueAccumulator = new HashMap<>();

    Optional<MappedExpressionUtils.ResolvedExpression<T>> preparedConditionExpression =
        prepare(liveMappingDescription, condition, idGenerator, shortCodeAccumulator);

    List<Statement<T>> temporaryStatements =
        extraSetAddRemoveExpressions.stream()
            .map(
                it ->
                    it.prepare(
                        idGenerator,
                        liveMappingDescription,
                        shortCodeAccumulator,
                        shortCodeValueAccumulator))
            .collect(Collectors.toList());

    TreeMap<String, Statement<T>> deduplicatedStatements = new TreeMap<>();
    temporaryStatements.forEach(it -> deduplicatedStatements.put(it.getPath().serialize(), it));

    Map<String, String> attributes =
        deduplicatedStatements.values().stream()
            .map(Statement::getAttributes)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

    Map<String, String> attributesFromConditions =
        preparedConditionExpression
            .map(MappedExpressionUtils.ResolvedExpression::getAttributes)
            .orElse(Collections.emptyMap());

    Map<String, String> totalAttributesAll =
        Stream.of(attributes, attributesFromConditions)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

    Map<String, AttributeValue> valuesFromStatements =
        deduplicatedStatements.values().stream()
            .map(Statement::getValues)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

    Map<String, AttributeValue> allValuesFromCondition =
        preparedConditionExpression
            .map(MappedExpressionUtils.ResolvedExpression::getValues)
            .orElse(Collections.emptyMap());

    Map<String, AttributeValue> allValues =
        Stream.of(valuesFromStatements, allValuesFromCondition)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    String setExpr =
        deduplicatedStatements.values().stream()
            .filter(UpdateStatement.class::isInstance)
            .map(Statement::serialize)
            .collect(Collectors.joining(" , "));

    String addExpr =
        deduplicatedStatements.values().stream()
            .filter(AddStatement.class::isInstance)
            .map(Statement::serialize)
            .collect(Collectors.joining(" , "));

    String removeExpr =
        deduplicatedStatements.values().stream()
            .filter(RemoveStatement.class::isInstance)
            .map(Statement::serialize)
            .collect(Collectors.joining(" , "));

    return UpdateItemRequest.builder()
        .key(keys)
        .expressionAttributeValues(allValues.isEmpty() ? null : allValues)
        .updateExpression(
            serializeEpr("SET", setExpr)
                + " "
                + serializeEpr("ADD", addExpr)
                + " "
                + serializeEpr("REMOVE", removeExpr))
        .expressionAttributeNames(totalAttributesAll)
        .conditionExpression(
            preparedConditionExpression
                .map(MappedExpressionUtils.ResolvedExpression::getExpression)
                .map(LogicalExpression::serialize)
                .orElse(null))
        .tableName(tableName)
        .build();
  }

  public Update asTransactionUpdate() {
    UpdateItemRequest update = asUpdateItemRequest();
    return Update.builder()
        .key(update.key())
        .tableName(tableName)
        .conditionExpression(update.conditionExpression())
        .expressionAttributeValues(update.expressionAttributeValues())
        .expressionAttributeNames(update.expressionAttributeNames())
        .updateExpression(update.updateExpression())
        .build();
  }

  private static String serializeEpr(String name, String value) {
    if (value.isEmpty()) return "";
    return name + " " + value;
  }

  public MappedUpdateExpression<T, G> condition(Function<G, LogicalExpression<T>> condition) {
    return this.withCondition(condition.apply(generator));
  }

  public static class RczSetExpressionGenerator<T> {
    public RczSetExpression<T> minus(RczPathExpression<T> a, RczPathExpression<T> b) {
      return new RczMathExpression<>(a, b, "-");
    }

    public RczSetExpression<T> plus(RczPathExpression<T> a, RczPathExpression<T> b) {
      return new RczMathExpression<>(a, b, "+");
    }

    public RczPathExpression<T> just(Path<T> source) {
      return new RczPathExpression<>(source);
    }

    public RczSetExpression<T> just(AttributeValue value) {
      return new RczValueExpression<>(value);
    }

    public RczSetExpression<T> just(String value) {
      return new RczValueExpression<>(AttributeValue.fromS(value));
    }
  }

  public interface RczSimpleExpression<T> extends RczSetExpression<T> {}

  public interface RczSetExpression<T> {

    String serialize();

    Map<String, AttributeValue> getValues();

    Map<String, String> getAttributes();

    RczSetExpression<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulator);
  }

  @AllArgsConstructor
  @RequiredArgsConstructor
  public static class RczValueExpression<T> implements RczSimpleExpression<T> {
    final AttributeValue attributeValue;
    @With String shortCodeValue;
    @With Map<String, AttributeValue> shortCodeValueAcumulator;

    @Override
    public Map<String, AttributeValue> getValues() {
      return Collections.singletonMap(shortCodeValue, shortCodeValueAcumulator.get(shortCodeValue));
    }

    @Override
    public Map<String, String> getAttributes() {
      return Collections.emptyMap();
    }

    @Override
    public String serialize() {
      return shortCodeValue;
    }

    @Override
    public RczValueExpression<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulatorArg) {

      String shortCode = ":" + idGenerator.get();

      shortCodeValueAccumulatorArg.put(shortCode, attributeValue);

      return this.withShortCodeValue(shortCode)
          .withShortCodeValueAcumulator(shortCodeValueAccumulatorArg);
    }
  }

  @AllArgsConstructor
  @Getter
  @RequiredArgsConstructor
  public static class RczPathExpression<T> implements RczSimpleExpression<T> {

    final Path<T> path;
    @With Map<String, String> shortCodeAccumulator;

    @Override
    public String serialize() {
      return path.serializeAsPartExpression(shortCodeAccumulator);
    }

    @Override
    public Map<String, AttributeValue> getValues() {
      return Collections.emptyMap();
    }

    @Override
    public RczPathExpression<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulator) {

      path.getPartsName()
          .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));

      return this.withShortCodeAccumulator(shortCodeAccumulator);
    }

    @Override
    public Map<String, String> getAttributes() {

      Set<String> parts = path.getPartsName();
      return shortCodeAccumulator.entrySet().stream()
          .filter(it -> parts.contains(it.getKey()))
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
  }

  @With
  @AllArgsConstructor
  public static class RczMathExpression<T> implements RczSetExpression<T> {
    RczPathExpression<T> a;
    RczPathExpression<T> b;
    String operation;

    @Override
    public String serialize() {
      return String.format(" %s %s %s", a.serialize(), operation, b.serialize());
    }

    @Override
    public RczMathExpression<T> prepare(
        ConsecutiveIdGenerator idGenerator,
        LiveMappingDescription<T> liveMappingDescription,
        Map<String, String> shortCodeAccumulator,
        Map<String, AttributeValue> shortCodeValueAccumulator) {
      return this.withA(
              a.prepare(
                  idGenerator,
                  liveMappingDescription,
                  shortCodeAccumulator,
                  shortCodeValueAccumulator))
          .withB(
              b.prepare(
                  idGenerator,
                  liveMappingDescription,
                  shortCodeAccumulator,
                  shortCodeValueAccumulator));
    }

    @Override
    public Map<String, String> getAttributes() {

      return Stream.of(a, b)
          .map(RczSetExpression::getAttributes)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (tmp1, tmp2) -> tmp1));
    }

    @Override
    public Map<String, AttributeValue> getValues() {
      return Stream.of(a, b)
          .map(RczSetExpression::getValues)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (tmp1, tmp2) -> tmp1));
    }
  }
}
