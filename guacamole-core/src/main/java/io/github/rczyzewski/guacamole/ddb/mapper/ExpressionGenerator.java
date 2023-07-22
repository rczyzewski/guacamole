package io.github.rczyzewski.guacamole.ddb.mapper;

import io.github.rczyzewski.guacamole.ddb.path.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExpressionGenerator<T> {
    @Getter
    @AllArgsConstructor
    public enum AttributeType {
        STRING("S"),
        NUMBER("N"),
        LIST("L"),
        MAP("M"),
        OBJECT("M"),
        SET("S");
        final String type;
    }
    public LogicalExpression<T> isAttributeType(Path<T> path, AttributeType type){
        return new LogicalExpression.AttributeType<>(path, type);
    }

    public LogicalExpression<T> exists(Path<T> path){
        return new LogicalExpression.AttributeExists<>(true, path);
    }
    public LogicalExpression<T> notExists(Path<T> path){
        return new LogicalExpression.AttributeExists<>(false, path);
    }
    public LogicalExpression<T> compare(Path<T> path1, LogicalExpression.ComparisonOperator op, Path<T> path2) {
        //TODO: it must take Path<>
        return LogicalExpression.ComparisonToReference.<T>builder()
                .otherFieldName(path2.serialize())
                .fieldName(path1.serialize())
                .operator(op)
                .build();
    }

    public LogicalExpression<T> compare(Path<T> path1, LogicalExpression.ComparisonOperator op, Double value) {
        return this.compare(path1, op, AttributeValue.fromN(Double.toString(value)));
    }

    public LogicalExpression<T> compare(Path<T> path1, LogicalExpression.ComparisonOperator op, Long value) {
        return this.compare(path1, op, AttributeValue.fromN(Long.toString(value)));
    }

    public LogicalExpression<T> compare(Path<T> path1, LogicalExpression.ComparisonOperator op, Float value) {
        return this.compare(path1, op, AttributeValue.fromN(Float.toString(value)));
    }

    public LogicalExpression<T> compare(Path<T> path1, LogicalExpression.ComparisonOperator op, Integer value) {
        return this.compare(path1, op, AttributeValue.fromN(Integer.toString(value)));
    }

    public LogicalExpression<T> compare(Path<T> path1, LogicalExpression.ComparisonOperator op, AttributeValue value) {
        return LogicalExpression.ComparisonToValue.<T>builder()
                .dynamoDBEncodedValue(value)
                .fieldName(path1.serialize())
                .operator(op)
                .build();
    }

    public LogicalExpression<T> compare(Path<T> path1, LogicalExpression.ComparisonOperator op, String value) {
        return LogicalExpression.ComparisonToValue.<T>builder()
                .dynamoDBEncodedValue(AttributeValue.fromS(value))
                .fieldName(path1.serialize())
                .operator(op)
                .build();
    }

    public LogicalExpression<T> and(List<LogicalExpression<T>> l1) {
        return new LogicalExpression.AndExpression<>(l1);
    }

    public LogicalExpression<T> and(LogicalExpression<T> l1) {
        return and(Collections.singletonList(l1));
    }

    public LogicalExpression<T> and(LogicalExpression<T> l1, LogicalExpression<T> l2) {
        return and(Arrays.asList(l1, l2));
    }

    public LogicalExpression<T> and(LogicalExpression<T> l1,
                                    LogicalExpression<T> l2,
                                    LogicalExpression<T> l3) {
        return and(Arrays.asList(l1, l2, l3));
    }

    public LogicalExpression<T> or(List<LogicalExpression<T>> l1) {
        return new LogicalExpression.OrExpression<>(l1);

    }

    public LogicalExpression<T> or(LogicalExpression<T> l1) {
        return or(Collections.singletonList(l1));
    }

    public LogicalExpression<T> or(LogicalExpression<T> l1, LogicalExpression<T> l2) {
        return or(Arrays.asList(l1, l2));
    }

    public LogicalExpression<T> or(LogicalExpression<T> l1,
                                   LogicalExpression<T> l2,
                                   LogicalExpression<T> l3) {
        return or(Arrays.asList(l1, l2, l3));
    }

    public LogicalExpression<T> not(LogicalExpression<T> arg) {
        return LogicalExpression.NotExpression.build(arg);
    }

}
