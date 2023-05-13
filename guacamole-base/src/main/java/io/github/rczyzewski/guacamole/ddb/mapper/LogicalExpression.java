package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface LogicalExpression<T>
{
    String serialize();
    LogicalExpression<T> prepare(ConsecutiveIdGenerator idGenerator);
    Map<String, AttributeValue> getValuesMap();



    @RequiredArgsConstructor
    @AllArgsConstructor
    @With
     class LessThanExpression<K> implements LogicalExpression<K>
    {
        final String fieldName;
        final Integer value;
        String shortValueCode;
        AttributeValue dynamoDBEncodedValue;


        @Override
        public String serialize()
        {
            return String.format(" %s < %s", fieldName, shortValueCode);
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator)
        {
            return this.withShortValueCode(":" + idGenerator.get())
                       .withDynamoDBEncodedValue( AttributeValue.fromN(Integer.toString(value)));
        }

        @Override
        public Map<String, AttributeValue> getValuesMap()
        {
            return Collections.singletonMap(shortValueCode, dynamoDBEncodedValue);
        }
    }

    @AllArgsConstructor
    @RequiredArgsConstructor
    @With
    class Equal<K> implements LogicalExpression<K>
    {
        final String fieldName;
        final String value;
        String shortValueCode;
        AttributeValue dynamoDBEncodedValue;

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator)
        {
            return this.withShortValueCode(":"+ idGenerator.get())
                       .withDynamoDBEncodedValue( AttributeValue.fromS(fieldName));
        }

        @Override
        public Map<String, AttributeValue> getValuesMap()
        {
            return Collections.singletonMap(shortValueCode, dynamoDBEncodedValue);
        }

        @Override
        public String serialize()
        {
            return String.format(" %s = %s", fieldName, shortValueCode);
        }
    }

    @AllArgsConstructor(staticName = "build")
    @With
    class OrExpression<K> implements LogicalExpression<K>
    {
        List<LogicalExpression<K>> args;

        @Override
        public String serialize()
        {
            return args.stream()
                       .map(LogicalExpression::serialize)
                       .map(it->String.format( "( %s )", it))
                       .collect(Collectors.joining(" or "));
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator)
        {
            return this.withArgs(args.stream().map(it->it.prepare(idGenerator)).collect(Collectors.toList()));
        }

        @Override
        public Map<String, AttributeValue> getValuesMap()
        {
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
    class AndExpression<K> implements LogicalExpression<K>
    {
        List<LogicalExpression<K>> args;

        @Override
        public String serialize()
        {
            return args.stream()
                       .map(LogicalExpression::serialize)
                       .map(it -> String.format("( %s )", it))
                       .collect(Collectors.joining(" and "));
        }
        public Map<String, AttributeValue> getValuesMap()
        {
            return args.stream()
                       .map(LogicalExpression::getValuesMap)
                       .map(Map::entrySet)
                       .flatMap(Collection::stream)
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator)
        {
            return this.withArgs(args.stream().map(it -> it.prepare(idGenerator)).collect(Collectors.toList()));
        }
    }
    @AllArgsConstructor(staticName = "build")
    @With
    class NotExpression<K> implements LogicalExpression<K>
    {
        LogicalExpression<K> arg;

        @Override
        public String serialize()
        {
            return null;

        }
        public Map<String, AttributeValue> getValuesMap()
        {
            return arg.getValuesMap();
        }
        @Override
        public LogicalExpression<K> prepare(ConsecutiveIdGenerator idGenerator)
        {
            return this.withArg(arg.prepare(idGenerator));
        }
    }
}