package io.github.rczyzewski.guacamole.ddb;

import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public interface BaseRepository<T>
{
    //TODO: rename to "put"
    PutItemRequest create(T item);
    DeleteItemRequest delete(T item);
    UpdateItemRequest update(T data);
    DynamoSearch getAll();
    CreateTableRequest createTable();
}
