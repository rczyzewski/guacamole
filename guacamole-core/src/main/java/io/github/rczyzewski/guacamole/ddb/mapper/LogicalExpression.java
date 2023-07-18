package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 condition-expression ::=
 operand comparator operand
 | operand BETWEEN operand AND operand
 | operand IN ( operand (',' operand (, ...) ))
 | function
 | condition AND condition
 | condition OR condition
 | NOT condition
 | ( condition )


 function ::=
 attribute_exists (path)
 | attribute_not_exists (path)
 | attribute_type (path, type)
 | begins_with (path, substr)
 | contains (path, operand)
 | size (path)
 **/


public interface LogicalExpression<T>{
    String serialize();
    LogicalExpression<T> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<T> liveMappingDescription, Map<String,String> shortCodeAccumulator);

    Map<String, AttributeValue> getValuesMap();
    Map<String, String> getAttributesMap();


    @AllArgsConstructor
    @RequiredArgsConstructor
    class AttributeExists<K> implements LogicalExpression<K>{

        final boolean shouldExists;
        final String fieldName;
        @With
        String fieldCode;

        @Override
        public String serialize(){
            if ( shouldExists ) {
             return String.format("attribute_exists(%s)" , this.fieldCode);

            }
            return String.format("attribute_not_exists(%s)" , this.fieldCode);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator,
                                            LiveMappingDescription<K> liveMappingDescription, Map<String,String> shortCodeAccumulator){

            String sk = liveMappingDescription.getDict().get(fieldName).getShortCode();
            return this.withFieldCode("#" + sk);
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getAttributesMap(){
            return Collections.singletonMap(fieldCode, fieldName);
        }
    }
    @RequiredArgsConstructor
    @AllArgsConstructor
    class AttributeType<K> implements LogicalExpression<K>{

        final String path;
        final ExpressionGenerator.AttributeType type;

        @With
        String fieldShortCode;
        @With
        String valueCode;

        @Override
        public String serialize(){
            return String.format("attribute_type( %s , %s )",  fieldShortCode, valueCode );
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<K> liveMappingDescription, Map<String, String> shortCodeAccumulator) {
            if (liveMappingDescription.getDict().containsKey(path)) {
                String sk = liveMappingDescription.getDict().get(path).getShortCode();
                String vk = idGenerator.get();
                return this.withFieldShortCode("#" + sk).withValueCode(":" + vk);

            } else if (!shortCodeAccumulator.containsKey(path)) {
                String kk = idGenerator.get();
                shortCodeAccumulator.put(path, kk);
            }
            String sk = shortCodeAccumulator.get(path);
            String vk = idGenerator.get();
            return this.withFieldShortCode("#" + sk).withValueCode(":" + vk);
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.singletonMap(this.valueCode, AttributeValue.fromS(type.getType()));
        }

        @Override
        public Map<String, String> getAttributesMap(){
            return Collections.singletonMap(fieldShortCode, path);
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
     enum ComparisonOperator{

        EQUAL("="),
        NOT_EQUAL("<>"),
        LESS("<"),
        LESS_OR_EQUAL("<="),
        GREATER(">"),
        GREATER_OR_EQUAL(">=");

        private final String symbol;
    }
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    class ComparisonToReference<K> implements LogicalExpression<K>{
        final String fieldName;
        final ComparisonOperator operator;
        final String otherFieldName;
        @With
        String otherFieldCode;
        @With
        String fieldCode;
        @Override
        public String serialize(){
            return String.format(" %s %s %s", fieldCode, operator.getSymbol(),  otherFieldCode);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator,
                                            LiveMappingDescription<K> liveMappingDescription,
                                            Map<String,String> shortCodeAccumulator){
            String sk = liveMappingDescription.getDict()
                    .get(fieldName)
                    .getShortCode();

            String sk2 = liveMappingDescription.getDict()
                    .get(otherFieldName)
                    .getShortCode();

            return this.withFieldCode("#" + sk).withOtherFieldCode("#" + sk2);
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getAttributesMap(){
            HashMap<String, String> returnValue = new HashMap<>();
            returnValue.put(this.fieldCode, this.fieldName);
            returnValue.put(this.otherFieldCode, this.otherFieldName);
            return returnValue;
        }
    }

    @Builder
    @AllArgsConstructor
    @RequiredArgsConstructor
    class ComparisonToValue<K> implements LogicalExpression<K>{
        final String fieldName;
        final ComparisonOperator operator;
        final AttributeValue dynamoDBEncodedValue;

        @With
        String shortValueCode;

        @With
        String fieldCode;

        @Override
        public String serialize(){
            return String.format(" %s %s %s", fieldCode, operator.getSymbol(), shortValueCode);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<K> liveMappingDescription, Map<String,String> shortCodeAccumulator){
            String sk = liveMappingDescription.getDict()
                                              .get(fieldName)
                                              .getShortCode();
            return this.withShortValueCode(":" + idGenerator.get())
                    .withFieldCode("#" + sk);

        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.singletonMap(shortValueCode, dynamoDBEncodedValue);
        }

        @Override
        public Map<String, String> getAttributesMap(){
            return Collections.singletonMap(this.fieldCode, this.fieldName);


        }
    }

    @AllArgsConstructor
    @With
    class OrExpression<K> implements LogicalExpression<K> {
        final List<LogicalExpression<K>> args;

        @Override
        public String serialize() {
            if( args.size() == 1 ) {
                return String.format( "%s " , args.get(0).serialize());
            } else {
                return args.stream()
                        .map(LogicalExpression::serialize)
                        .map(it -> String.format(" %s ", it))
                        .collect(Collectors.joining(" or ", "(", ")"));
            }
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator,
                                            LiveMappingDescription<K> liveMappingDescription,
                                            Map<String, String> shortCodeAccumulator) {
            return this.withArgs(args.stream().map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator))
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
    class AndExpression<K> implements LogicalExpression<K>{
        final  List<LogicalExpression<K>> args;

        @Override
        public String serialize(){
            if( args.size() == 1 ) {
               return String.format( "%s " , args.get(0).serialize());
            } else {
                return args.stream()
                        .map(LogicalExpression::serialize)
                        .map(it -> String.format(" %s ", it))
                        .collect(Collectors.joining(" and ", "(", ")"));
            }
        }

        public Map<String, AttributeValue> getValuesMap(){
            return args.stream()
                       .map(LogicalExpression::getValuesMap)
                       .map(Map::entrySet)
                       .flatMap(Collection::stream)
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public Map<String, String> getAttributesMap(){
            return args.stream()
                       .map(LogicalExpression::getAttributesMap)
                       .map(Map::entrySet)
                       .flatMap(Collection::stream)
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (s, s2)-> s));
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator,
                                            LiveMappingDescription<K> liveMappingDescription,
                                            Map<String,String> shortCodeAccumulator
        ){
            return this.withArgs(args.stream().map(it -> it.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator))
                                     .collect(Collectors.toList()));
        }
    }

    @With
    @AllArgsConstructor(staticName = "build")
    class NotExpression<K> implements LogicalExpression<K>{
        final LogicalExpression<K> arg;

        @Override
        public String serialize(){
            return String.format("NOT %s", arg.serialize());

        }

        public Map<String, AttributeValue> getValuesMap(){
            return arg.getValuesMap();
        }

        @Override
        public Map<String, String> getAttributesMap(){
            return arg.getAttributesMap();
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<K> liveMappingDescription, Map<String,String> shortCodeAccumulator){
            return this.withArg(arg.prepare(idGenerator, liveMappingDescription, shortCodeAccumulator));
        }
    }
}