package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    LogicalExpression<T> prepare(ConsecutiveIdGenerator idGenerator);

    Map<String, AttributeValue> getValuesMap();

    interface NumberExpression<T>{

        String serialize();

        NumberExpression<T> prepare(ConsecutiveIdGenerator idGenerator);

        Map<String, AttributeValue> getValuesMap();
    }

    @AllArgsConstructor
    class FixedNumberExpression<T> implements NumberExpression<T>{

        String arg;

        public String serialize(){
            return arg;
        }

        public NumberExpression<T> prepare(ConsecutiveIdGenerator idGenerator){
            return this;
        }

        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
        }
    }

    @With
    @AllArgsConstructor
    class CompoundCompariseExpression<T> implements LogicalExpression<T>{
        NumberExpression<T> a;
        ComparisonOperator operator;
        NumberExpression<T> b;

        @Override
        public String serialize(){
            return a.serialize() + " " + operator.getSymbol() + b.serialize();
        }

        @Override
        public LogicalExpression<T> prepare(ConsecutiveIdGenerator idGenerator){
            return this.withA(a.prepare(idGenerator))
                       .withB(b.prepare(idGenerator));
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Stream.of(a, b)
                         .map(NumberExpression::getValuesMap)
                         .map(Map::entrySet)
                         .flatMap(Collection::stream)
                         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    @AllArgsConstructor
    @With
    class AttributeExists<K> implements LogicalExpression<K>{

        final boolean shouldExists;
        final String fieldName;

        @Override
        public String serialize(){
            if ( shouldExists ) {
             return String.format("attribute_not_exists(%s)" , this.fieldName);

            }
            return String.format("attribute_exists(%s)" , this.fieldName);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator){
            //There is nothing to prepare
            return this;
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
        }
    }
    @AllArgsConstructor
    @With
    class AttributeType<K> implements LogicalExpression<K>{

        final boolean shouldExists;
        final String fieldName;

        @Override
        public String serialize(){
            if ( shouldExists ) {
                return String.format("attribute_not_exists(%s)" , this.fieldName);

            }
            return String.format("attribute_exists(%s)" , this.fieldName);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator){
            //There is nothing to prepare
            return this;
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
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
    @AllArgsConstructor
    @With
    class ComparisonToReference<K> implements LogicalExpression<K>{
        final String fieldName;
        final ComparisonOperator operator;
        final String otherFieldName;

        @Override
        public String serialize(){
            return String.format(" %s %s %s", fieldName, operator.getSymbol(),  otherFieldName);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator){
            return this;
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.emptyMap();
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

        @Override
        public String serialize(){
            return String.format(" %s %s %s", fieldName, operator.getSymbol(), shortValueCode);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator){
            return this.withShortValueCode(":" + idGenerator.get());
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return Collections.singletonMap(shortValueCode, dynamoDBEncodedValue);
        }
    }

    @AllArgsConstructor(staticName = "build")
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
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator){
            return this.withArgs(args.stream().map(it -> it.prepare(idGenerator)).collect(Collectors.toList()));
        }

        @Override
        public Map<String, AttributeValue> getValuesMap(){
            return args.stream()
                       .map(LogicalExpression::getValuesMap)
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
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator){
            return this.withArgs(args.stream().map(it -> it.prepare(idGenerator)).collect(Collectors.toList()));
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
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator){
            return this.withArg(arg.prepare(idGenerator));
        }
    }
}