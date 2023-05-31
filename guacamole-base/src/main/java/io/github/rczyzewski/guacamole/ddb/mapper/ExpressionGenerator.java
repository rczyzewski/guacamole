package io.github.rczyzewski.guacamole.ddb.mapper;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExpressionGenerator<T, E >
{
    protected E e;

    public LogicalExpression<T> and(List<LogicalExpression<T>> l1){
        return new LogicalExpression.AndExpression<>(l1);

    }
    public LogicalExpression<T> and(LogicalExpression<T> l1){
        return and(Collections.singletonList(l1));
    }
    public LogicalExpression<T> and(LogicalExpression<T> l1, LogicalExpression<T> l2){
        return and(Arrays.asList(l1, l2));
    }
    public LogicalExpression<T> and(LogicalExpression<T> l1,
                                    LogicalExpression<T> l2,
                                    LogicalExpression<T> l3){
        return and(Arrays.asList(l1, l2, l3));
    }


    public LogicalExpression<T> or(List<LogicalExpression<T>> l1){
        return new LogicalExpression.OrExpression<>(l1);

    }
    public LogicalExpression<T> or(LogicalExpression<T> l1){
        return or(Collections.singletonList(l1));
    }
    public LogicalExpression<T> or(LogicalExpression<T> l1, LogicalExpression<T> l2){
        return or(Arrays.asList(l1, l2));
    }
    public LogicalExpression<T> or(LogicalExpression<T> l1,
                                    LogicalExpression<T> l2,
                                    LogicalExpression<T> l3){
        return or(Arrays.asList(l1, l2, l3));
    }

    public LogicalExpression<T> not(LogicalExpression<T> arg){
        return LogicalExpression.NotExpression.build(arg);
    }


}
