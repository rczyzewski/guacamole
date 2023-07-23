package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public interface BaseRepository<T, G extends ExpressionGenerator<T>>
{
    PutItemRequest create(T item);

    MappedDeleteExpression<T, G> delete(T item);

    MappedUpdateExpression<T, G> update(T data);

    MappedScanExpression<T, G> scan();

    DynamoSearch getAll();

    CreateTableRequest createTable();
}
