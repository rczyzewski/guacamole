package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

public class LogicalExpression<T>
{

    @AllArgsConstructor()
    public static class LessThanExpression<K> extends LogicalExpression<K>
    {
        String fieldName;
        String value;
    }

    @AllArgsConstructor
    public static class Equal<K> extends LogicalExpression<K>
    {
        String field;
        String value;
    }

    @AllArgsConstructor(staticName = "build")
    public static class OrExpression<K> extends LogicalExpression<K>
    {
        List<LogicalExpression<K>> args;
    }

    @Builder
    @AllArgsConstructor
    public static class AndExpression<K> extends LogicalExpression<K>
    {
        List<LogicalExpression<K>> and;
    }

    @AllArgsConstructor(staticName = "build")
    public static class NotExpression<K> extends LogicalExpression<K>
    {
        LogicalExpression<K> args;
    }
}