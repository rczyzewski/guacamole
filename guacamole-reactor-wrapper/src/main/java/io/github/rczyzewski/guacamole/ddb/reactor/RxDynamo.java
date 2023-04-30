package io.github.rczyzewski.guacamole.ddb.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;

public interface RxDynamo
{
    Flux<Map<String, AttributeValue>> scan(@NonNull DynamoSearch dynamoSearch);

    Flux<Map<String, AttributeValue>> search(@NonNull DynamoSearch dynamoSearch);

    Mono<PutItemResponse> save(PutItemRequest putItemRequest);

    Mono<UpdateItemResponse> update(UpdateItemRequest updateItemRequest);

    Mono<DeleteItemResponse> delete(DeleteItemRequest deleteItemRequest);

    Mono<CreateTableResponse> createTable(
        CreateTableRequest createTableRequest);

    Mono<DeleteTableResponse> deleteTable(String name);
}
