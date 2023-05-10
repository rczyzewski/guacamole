package io.github.rczyzewski.guacamole.ddb;

import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public interface BaseRepository<T >
{


    PutItemRequest create(T item);
    DeleteItemRequest delete(T item);
    @Deprecated(since="2023-05-10")
    UpdateItemRequest update(T data);
    DynamoSearch getAll();
    CreateTableRequest createTable();
}
