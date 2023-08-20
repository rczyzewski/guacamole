package io.github.rczyzewski.guacamole.ddb;

import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public interface BaseRepository<T, G extends ExpressionGenerator<T>, S>
{
    PutItemRequest create(T item);
    LiveMappingDescription<T> getMapper();

    MappedDeleteExpression<T, G> delete(T item);

    MappedUpdateExpression<T, G> update(T data);

    MappedScanExpression<T, G> scan();

    CreateTableRequest createTable();
}
