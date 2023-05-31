package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Collections;
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
    LogicalExpression<T> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<T> liveMappingDescription);

    Map<String, AttributeValue> getValuesMap();
    Map<String, String> getAttributesMap();

    @Builder
    @AllArgsConstructor
    @Value
    @NonFinal
    class FixedExpression<T> implements  LogicalExpression<T>{
        String expression;

        @Singular("value")
        Map<String, AttributeValue> valuesMap;

        @Singular("attribute")
        Map<String, String> attributesMap = Collections.emptyMap();
        @Override
        public String serialize(){
            return this.getExpression();
        }

        @Override
        public LogicalExpression<T> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<T> liveMappingDescription){
            return this;
        }
    }

    @AllArgsConstructor
    @RequiredArgsConstructor
    @With
    class AttributeExists<K> implements LogicalExpression<K>{

        final boolean shouldExists;
        final String fieldName;
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
                                            LiveMappingDescription<K> liveMappingDescription){
            //There is nothing to prepare
            if(fieldCode != null)
                return this;
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
    @With
    class AttributeType<K> implements LogicalExpression<K>{

        final boolean shouldExists;
        final String fieldName;
        String fieldShortCode;

        @Override
        public String serialize(){
            if ( shouldExists ) {
                return String.format("attribute_not_exists(%s)" , this.fieldShortCode);
            }
            return String.format("attribute_exists(%s)" , this.fieldShortCode);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<K> liveMappingDescription){
            //There is nothing to prepare
            String sk = liveMappingDescription.getDict().get(fieldName).getShortCode();
            return this.withFieldShortCode("#" + sk);
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getAttributesMap(){
            return Collections.singletonMap(fieldShortCode, fieldName);
        }
    }

    /***
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
        LESS_THAN("<"),
        LESS_OR_EQUAL("<="),
        GREATER(">"),
        GREATER_OR_EQUAL(">=");

        private final String symbol;
    }
    @With
    @RequiredArgsConstructor
    @AllArgsConstructor
    class ComparisonToReference<K> implements LogicalExpression<K>{
        final String fieldName;
        final ComparisonOperator operator;
        final String otherFieldName;
        String fieldCode;

        @Override
        public String serialize(){
            return String.format(" %s %s %s", fieldName, operator.getSymbol(),  otherFieldName);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator,
                                            LiveMappingDescription<K> liveMappingDescription){
            return this;
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getAttributesMap(){
            throw new RuntimeException("not implemented yet");
        }
    }

    /***
     *   comparator ::=
     *       =
     *       | <>
     *       | <
     *       | <=
     *       | >
     *       | >=
     *
     */
    @RequiredArgsConstructor
    @AllArgsConstructor
    @With
    class ComparisonToValue<K> implements LogicalExpression<K>{
        final String fieldName;


        final ComparisonOperator operator;
        final AttributeValue dynamoDBEncodedValue;
        String shortValueCode;

        String fieldCode;

        @Override
        public String serialize(){
            return String.format(" %s %s %s", fieldCode, operator.getSymbol(), shortValueCode);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<K> liveMappingDescription){
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
    class OrExpression<K> implements LogicalExpression<K>{
        List<LogicalExpression<K>> args;

        @Override
        public String serialize(){
            return args.stream()
                       .map(LogicalExpression::serialize)
                       .map(it -> String.format("( %s )", it))
                       .collect(Collectors.joining(" or "));
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator ,
                                            LiveMappingDescription<K> liveMappingDescription){
            return this.withArgs(args.stream().map(it -> it.prepare(idGenerator, liveMappingDescription))
                                     .collect(Collectors.toList()));
        }

        @Override
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
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    @Builder
    @AllArgsConstructor
    @With
    class AndExpression<K> implements LogicalExpression<K>{
        List<LogicalExpression<K>> args;

        @Override
        public String serialize(){
            return args.stream()
                       .map(LogicalExpression::serialize)
                       .map(it -> String.format("( %s )", it))
                       .collect(Collectors.joining(" and "));
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
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator,
                                            LiveMappingDescription<K> liveMappingDescription){
            return this.withArgs(args.stream().map(it -> it.prepare(idGenerator, liveMappingDescription))
                                     .collect(Collectors.toList()));
        }
    }

    @AllArgsConstructor(staticName = "build")
    @With
    class NotExpression<K> implements LogicalExpression<K>{
        LogicalExpression<K> arg;

        @Override
        public String serialize(){
            return null;

        }

        public Map<String, AttributeValue> getValuesMap(){
            return arg.getValuesMap();
        }

        @Override
        public Map<String, String> getAttributesMap(){
            return arg.getAttributesMap();
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator, LiveMappingDescription<K> liveMappingDescription){
            return this.withArg(arg.prepare(idGenerator, liveMappingDescription));
        }
    }
}