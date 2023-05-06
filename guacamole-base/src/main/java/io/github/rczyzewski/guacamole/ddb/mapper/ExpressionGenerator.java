package io.github.rczyzewski.guacamole.ddb.mapper;


import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpressionGenerator<T, E >
{
    protected E e;

    public LogicalExpression<T> and( Function<E, LogicalExpression<T>> l1)
    {
        return new LogicalExpression.AndExpression<>(List.of(l1.apply(e) ));
    }

    public LogicalExpression<T> and(Function<E, LogicalExpression<T>> l1, Function<E, LogicalExpression<T>> l)
    {
        return new LogicalExpression.AndExpression<>(Stream.of(l1, l).map(it -> it.apply(e)).collect(Collectors.toList()));
    }

    public LogicalExpression<T> or(Function<E, LogicalExpression<T>> f, Function<E, LogicalExpression<T>> f2)
    {
        return LogicalExpression.OrExpression.build(List.of(f.apply(e),f2.apply(e)) );
    }


    public LogicalExpression<T> not(LogicalExpression<T> arg)
    {
        return LogicalExpression.NotExpression.build(arg);
    }


}
