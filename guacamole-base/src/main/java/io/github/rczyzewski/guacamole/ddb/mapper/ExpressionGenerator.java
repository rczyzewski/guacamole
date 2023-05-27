package io.github.rczyzewski.guacamole.ddb.mapper;


import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpressionGenerator<T, E >
{
    protected E e;

    LogicalExpression<T> compare(LogicalExpression.NumberExpression<T> a,
                                 LogicalExpression.ComparisonOperator op,
                                 LogicalExpression.NumberExpression<T> b ){
        return new LogicalExpression.CompoundCompariseExpression<>(a , op ,b);
    }

    public LogicalExpression<T> and( LogicalExpression<T> l1)
    {
        return new LogicalExpression.AndExpression<>(Collections.singletonList(l1));
    }

    public LogicalExpression<T> and( LogicalExpression<T> l1, LogicalExpression<T> l)
    {
        return new LogicalExpression.AndExpression<>(Stream.of(l1, l).collect(Collectors.toList()));
    }

    public LogicalExpression<T> or(LogicalExpression<T> f, LogicalExpression<T> f2)
    {
        return LogicalExpression.OrExpression.build(Arrays.asList(f, f2) );
    }


    public LogicalExpression<T> not(LogicalExpression<T> arg)
    {
        return LogicalExpression.NotExpression.build(arg);
    }


}
