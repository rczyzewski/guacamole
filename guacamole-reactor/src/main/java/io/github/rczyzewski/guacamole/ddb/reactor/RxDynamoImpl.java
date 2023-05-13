package io.github.rczyzewski.guacamole.ddb.reactor;

import io.github.rczyzewski.guacamole.ddb.DynamoSearch;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

@AllArgsConstructor
public class RxDynamoImpl implements RxDynamo
{

    private final DynamoDbAsyncClient ddbClient;

    public Flux<Map<String, AttributeValue>> scan(@NonNull DynamoSearch dynamoSearch)
    {
        ScanRequest.Builder scanRequest = ScanRequest.builder()
            .tableName(dynamoSearch.getTableName())
            .scanFilter(dynamoSearch.getFilterConditions())
            .expressionAttributeValues(dynamoSearch.getExpressionAttributeValues());
        Optional.ofNullable(dynamoSearch.getIndexName()).ifPresent(scanRequest::indexName);

        return Flux.from(ddbClient.scanPaginator(scanRequest.build()))
            .flatMapIterable(ScanResponse::items);
    }

    private Flux<Map<String, AttributeValue>> query(@NonNull DynamoSearch dynamoSearch)
    {
        QueryRequest.Builder queryRequest = QueryRequest.builder()
            .tableName(dynamoSearch.getTableName())
            .keyConditions(dynamoSearch.getKeyConditions())
            .queryFilter(dynamoSearch.getFilterConditions())
            .expressionAttributeValues(dynamoSearch.getExpressionAttributeValues());

        Optional.ofNullable(dynamoSearch.getIndexName()).ifPresent(queryRequest::indexName);

        return Flux.from(ddbClient.queryPaginator(queryRequest.build()))
            .flatMapIterable(QueryResponse::items);
    }

    @Override
    public Flux<Map<String, AttributeValue>> search(@NonNull DynamoSearch dynamoSearch)
    {
        return Optional.of(dynamoSearch)
            .map(DynamoSearch::getKeyConditions)
            .filter(it -> !it.isEmpty())
            .map(it -> dynamoSearch)
            .map(this::query)
            .orElseGet(() -> this.scan(dynamoSearch));
    }

    @Override
    public Mono<PutItemResponse> save(@NonNull PutItemRequest putItemRequest)
    {
        return Mono.just(putItemRequest)
            .log("PUT " + putItemRequest.tableName(), Level.FINER, SignalType.ON_NEXT)
            .map(ddbClient::putItem)
            .flatMap(Mono::fromFuture);
    }

    @Override
    public Mono<UpdateItemResponse> update(@NonNull UpdateItemRequest updateItemRequest)
    {
        return Mono.just(updateItemRequest)
            .log("UPDATE " + updateItemRequest.tableName(), Level.FINER, SignalType.ON_NEXT)
            .map(ddbClient::updateItem)
            .flatMap(Mono::fromFuture);
    }

    @Override
    public Mono<DeleteItemResponse> delete(@NonNull DeleteItemRequest deleteItemRequest)
    {
        return Mono.just(deleteItemRequest)
                .map(ddbClient::deleteItem)
                .flatMap(Mono::fromFuture);
    }

    @Override
    public Mono<CreateTableResponse> createTable(
        CreateTableRequest createTableRequest)
    {
        return Mono.just(createTableRequest)
                .map(ddbClient::createTable)
                .flatMap(Mono::fromFuture);
    }

    @Override
    public Mono<DeleteTableResponse> deleteTable(String table)
    {

      return  Mono.just(table)
                .map(it-> DeleteTableRequest.builder().tableName(table).build())
                .map( ddbClient::deleteTable)
                .flatMap(Mono::fromFuture);
    }
}
