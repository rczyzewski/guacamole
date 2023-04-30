package io.github.rczyzewski.guacamole.ddb.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;

public interface BaseRepository<T>
{
    Mono<T> create(T item);

    Mono<Void> delete(T item);

    Flux<T> getAll();

    Mono<T> update(T data);

    CreateTableRequest createTable();
}
