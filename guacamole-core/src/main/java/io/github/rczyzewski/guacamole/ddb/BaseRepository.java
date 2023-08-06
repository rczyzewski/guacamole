package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.function.Function;

public interface BaseRepository<BEAN_TYPE,
        EXPRESSION_GENERATOR extends ExpressionGenerator<BEAN_TYPE>,
        INDEX_SELECTOR >
{
    PutItemRequest create(BEAN_TYPE item);

    MappedDeleteExpression<BEAN_TYPE, EXPRESSION_GENERATOR> delete(BEAN_TYPE item);

    MappedUpdateExpression<BEAN_TYPE, EXPRESSION_GENERATOR> update(BEAN_TYPE data);

    MappedScanExpression<BEAN_TYPE, EXPRESSION_GENERATOR> scan();

    MappedQueryExpression<BEAN_TYPE, EXPRESSION_GENERATOR> query(Function<INDEX_SELECTOR,String> keyConditions);

 //   DynamoSearch getAll();

    CreateTableRequest createTable();
}
