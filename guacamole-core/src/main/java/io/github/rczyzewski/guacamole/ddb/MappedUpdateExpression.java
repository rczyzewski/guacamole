package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.*;
import io.github.rczyzewski.guacamole.ddb.path.Path;
import lombok.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.rczyzewski.guacamole.ddb.MappedExpressionUtils.prepare;



@Builder(toBuilder = true)
@AllArgsConstructor
public class MappedUpdateExpression<T, G extends ExpressionGenerator<T>>
{

    private final G generator;
    private final String tableName;
    private final Map<String, AttributeValue> keys;
    @With
    private final LogicalExpression<T> condition;

    @Builder.Default
    private  final List<UpdateStatement<T>> extraSetExpressions = new ArrayList<>();
    @Singular(value="add")
    private  Map<Path<T> , AttributeValue> addExpressions;
    @Singular(value="remove")
    private  List<Path<T>> remove;
    @Singular(value="delete")
    private  Map<Path<T>, UpdateExpression.ConstantValue> deleteExpressions;

    public MappedUpdateExpression<T, G> setIfEmpty(Path<T> path, Function<RczSetExpressionGenerator<T>, RczSimpleExpression<T>> expr) {
        RczSetExpressionGenerator<T> eg = new RczSetExpressionGenerator<>();

        extraSetExpressions.add(UpdateStatement.<T>builder()
                .path(path)
                .override(false)
                .value(expr.apply(eg))
                .build());
        return this;
    }
    public MappedUpdateExpression<T, G> set(Path<T> path, Function<RczSetExpressionGenerator<T>, RczSetExpression<T>> expr) {
        RczSetExpressionGenerator<T> eg = new RczSetExpressionGenerator<>();

        extraSetExpressions.add(UpdateStatement.<T>builder()
                .path(path)
                .override(true)
                .value(expr.apply(eg))
                .build());
        return this;
    }
    @Builder
    @With
    @Getter
    public static final  class UpdateStatement<T>{
       Path<T>  path;
        RczSetExpression<T> value;

        @Builder.Default
        boolean override = true;

        public String serialize(Map<String,String > shortCodeAccumulator) {

            if (override) {
                return path.serializeAsPartExpression(shortCodeAccumulator) + " = " + value.serialize();
            }
            String serializedPath = path.serializeAsPartExpression(shortCodeAccumulator);
            return String.format(" %s = if_not_exists( %s , %s )", serializedPath, serializedPath, value.serialize());
        }
         Map<String, String> getAttributes(Map<String, String> shortCodeAccumulator){

             Set<String> parts = path.getPartsName();
             Map<String, String> fromPath = shortCodeAccumulator.entrySet().stream().filter(it -> parts.contains(it.getKey()))
                     .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

           return Stream.of(fromPath, value.getAttributes())
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b )-> a));
        }
    }

    private final LiveMappingDescription<T> liveMappingDescription;
    public UpdateItemRequest asUpdateItemRequest()
    {
        ConsecutiveIdGenerator idGenerator = ConsecutiveIdGenerator.builder().base("ABCDEFGH").build();

        Map<String, String> shortCodeAccumulator = new HashMap<>();
        Map<String, AttributeValue> shortCodeValueAccumulator = new HashMap<>();

        Optional<MappedExpressionUtils.ResolvedExpression<T>> preparedConditionExpression =
                prepare(liveMappingDescription, condition, idGenerator, shortCodeAccumulator);

        TreeMap<String, UpdateStatement<T>> tmp = new TreeMap<>();
        extraSetExpressions.stream()
                .forEachOrdered(it-> tmp.put(it.path.serialize(), it));
        Collection<UpdateStatement<T>> deduplicatedSetExpressions = tmp.values();


        deduplicatedSetExpressions.stream()
                .map(it -> it.path)
                .map(Path::getPartsName)
                .flatMap(Collection::stream)
                .forEach(it -> shortCodeAccumulator.computeIfAbsent(it, ignored -> "#"+ idGenerator.get()));

        List<UpdateStatement<T>> preparedExpessions = deduplicatedSetExpressions.stream()
                .map(it -> it.withValue(it.value.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator, shortCodeValueAccumulator)))
                .collect(Collectors.toList());



        Map<String, String> attributes = preparedExpessions.stream()
                        .map(it->it.getAttributes(shortCodeAccumulator))
                        .map(Map::entrySet)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b)-> a));

        Map<String, String> attributesFromConditions = preparedConditionExpression
                .map(MappedExpressionUtils.ResolvedExpression::getAttributes)
                .orElse(Collections.emptyMap());

        Map<String, String> totalAttributesAll = Stream.of( attributes, attributesFromConditions)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(  a, b) -> a ));



        Map<String, AttributeValue> ddd = preparedExpessions.stream()
                .map(it -> it.value)
                .map(RczSetExpression::getValues)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, AttributeValue> allValues =
                        preparedConditionExpression
                                .map(MappedExpressionUtils.ResolvedExpression::getValues)
                                .orElse(Collections.emptyMap());

        Map<String, AttributeValue> toatlnieAllValues = Stream.of(ddd, allValues)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String setExpr = preparedExpessions.stream()
                .map(it-> it.serialize(shortCodeAccumulator))
                .collect(Collectors.joining(","));


        return UpdateItemRequest.builder()
                .key(keys)
                .expressionAttributeValues(toatlnieAllValues.isEmpty() ? null : toatlnieAllValues)
          .updateExpression("SET " + setExpr)
               .expressionAttributeNames(totalAttributesAll)
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

    public static  class RczSetExpressionGenerator<T> {
         public RczSetExpression<T> minus(RczPathExpression<T> a, RczPathExpression<T> b) {
            return new RczMathExpression<>(a, b, "-");
        }

         public RczSetExpression<T> plus(RczPathExpression<T> a, RczPathExpression<T> b) {
            return new RczMathExpression<>(a ,  b, "+");
        }

        public   RczPathExpression<T> just(Path<T> source) {
            return new RczPathExpression<>(source);
        }
        public   RczSetExpression<T> just(AttributeValue value) {
            return new RczValueExpression<>(value);
        }
        public   RczSetExpression<T> just(String value) {
            return new RczValueExpression<>(AttributeValue.fromS(value));
        }
        public   RczSetExpression<T> just(Integer value) {
            return new RczValueExpression<>(AttributeValue.fromS(Integer.toString(value)));
        }
    }
    public  interface RczSimpleExpression<T> extends RczSetExpression<T>{}

    public  interface RczSetExpression<T>{

        String serialize();

        Map<String, AttributeValue> getValues();
        Map<String, String> getAttributes();

        RczSetExpression<T> prepare(ConsecutiveIdGenerator idGenerator,
                          LiveMappingDescription<T> liveMappingDescription,
                          Map<String,String> shortCodeAccumulator,
                          Map<String,AttributeValue> shortCodeValueAccumulator

        );

    }
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class RczValueExpression<T> implements RczSimpleExpression<T> {
        final AttributeValue attributeValue;
        @With
        String shortCodeValue;
        @With
        Map<String, AttributeValue> shortCodeValueAcumulator;

        @Override
        public Map<String, AttributeValue> getValues(){
            return Collections.singletonMap(shortCodeValue, shortCodeValueAcumulator.get(shortCodeValue));
        }

        @Override
        public Map<String, String> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public String serialize() {
            return  shortCodeValue;
        }

        @Override
        public RczSetExpression<T> prepare(ConsecutiveIdGenerator idGenerator,
                                           LiveMappingDescription<T> liveMappingDescription,
                                           Map<String, String> shortCodeAccumulator,
                                           Map<String, AttributeValue> shortCodeValueAccumulatorArg

        ) {
            String shortCode = ":" +  idGenerator.get();

            shortCodeValueAccumulatorArg.put(shortCode, attributeValue);

            return this.withShortCodeValue(shortCode)
                    .withShortCodeValueAcumulator(shortCodeValueAccumulatorArg);
        }

    }
    @RequiredArgsConstructor
    public static class RczFunctionExpression<T> implements RczSetExpression<T> {
        final AttributeValue attributeValue;

        @Override
        public String serialize() {
            return null;
        }

        @Override
        public Map<String, AttributeValue> getValues() {
            return null;
        }

        @Override
        public Map<String, String> getAttributes() {
            return null;
        }

        @Override
        public RczSetExpression<T> prepare(ConsecutiveIdGenerator idGenerator,
                                           LiveMappingDescription<T> liveMappingDescription,
                                           Map<String, String> shortCodeAccumulator,
                                           Map<String, AttributeValue> shortCodeValueAccumulator

        ) {
            return this;
        }

    }
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class RczPathExpression<T> implements RczSimpleExpression<T> {
        final Path<T> path;
        @With
        Map<String, String> shortCodeAccumulator;

        @Override
        public String serialize() {
            return  path.serializeAsPartExpression(shortCodeAccumulator);
        }

        @Override
        public Map<String, AttributeValue> getValues() {
            return Collections.emptyMap();
        }

        @Override
        public RczPathExpression<T> prepare(ConsecutiveIdGenerator idGenerator,
                                           LiveMappingDescription<T> liveMappingDescription,
                                           Map<String, String> shortCodeAccumulator,
                                           Map<String, AttributeValue> shortCodeValueAccumulator

        ) {
            path.getPartsName()
                    .forEach(it-> shortCodeAccumulator.computeIfAbsent(it, ignored -> idGenerator.get()));

            return this.withShortCodeAccumulator(shortCodeAccumulator);
        }

        @Override
        public Map<String, String> getAttributes(){

            Set<String> parts = path.getPartsName();
            return shortCodeAccumulator.entrySet().stream().filter(it -> parts.contains(it.getKey()))
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
            return  String.format(" %s %s %s", a.serialize(),  operation , b.serialize());
        }

        @Override
        public RczMathExpression<T> prepare(ConsecutiveIdGenerator idGenerator,
                                           LiveMappingDescription<T> liveMappingDescription,
                                           Map<String, String> shortCodeAccumulator,
                                           Map<String, AttributeValue> shortCodeValueAccumulator
        ) {
          return this.withA(a.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator, shortCodeValueAccumulator))
            .withB(b.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator, shortCodeValueAccumulator));
        }
        @Override
        public Map<String, String> getAttributes(){

            return Stream.of(a, b)
                    .map(RczSetExpression::getAttributes)
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(tmp1, tmp2)-> tmp1 ));
        }
        @Override
        public Map<String, AttributeValue> getValues(){
            return Stream.of(a,b)
                    .map(RczSetExpression::getValues)
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(tmp1, tmp2)-> tmp1 ));
        }
    }
}
