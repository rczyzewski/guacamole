package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import lombok.AllArgsConstructor;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static io.github.rczyzewski.guacamole.ddb.MappedExpressionUtils.prepare;

@AllArgsConstructor
public class MappedScanExpression<T, G extends ExpressionGenerator<T>>{
    private final G generator;
    private final String tableName;
    @With
    private final LogicalExpression<T> condition;
    private final LiveMappingDescription<T> liveMappingDescription;

    public MappedScanExpression<T, G> condition(Function<G, LogicalExpression<T>> condition)
    {
        LogicalExpression<T> a = condition.apply(this.generator);
        return this.withCondition(a);
    }

    public ScanRequest asScanItemRequest() {

        Optional<MappedExpressionUtils.ResolvedExpression<T>> preparedConditionExpression = prepare(liveMappingDescription, condition);

        Map<String, String> allAttributeNames = preparedConditionExpression
                .map(MappedExpressionUtils.ResolvedExpression::getAttributes)
                .orElse(Collections.emptyMap());

        return ScanRequest.builder()
                .expressionAttributeValues(preparedConditionExpression
                        .map(MappedExpressionUtils.ResolvedExpression::getValues)
                        .orElse(null)
                )
                .expressionAttributeNames(allAttributeNames)
                .select(Select.ALL_ATTRIBUTES)
                .filterExpression(preparedConditionExpression
                        .map(MappedExpressionUtils.ResolvedExpression::getExpression)
                        .map(LogicalExpression::serialize)
                        .orElse(null))
                .tableName(tableName)
                .build();
    }
}
