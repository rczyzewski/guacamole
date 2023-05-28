package io.github.rczyzewski.guacamole.examples.it;

import io.github.rczyzewski.guacamole.examples.TestHelperDynamoDB;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamo;
import io.github.rczyzewski.guacamole.examples.shop.Product;
import io.github.rczyzewski.guacamole.examples.shop.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@Testcontainers
class ConditionalUpdateIT{
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

    private final RxDynamo rxDynamo = new RxDynamo(ddbClient);

    ProductRepository repo = new ProductRepository(getTableName());

    @Test
    void veryFirstTestForConditionalUpdate(){

        rxDynamo.createTable(repo.createTable())
                .block();

        Product a = Product
                .builder()
                .uid("2028JGG")
                .name("Millennium Falcon")
                .description("It's the property of Han Solo.")
                .price(8_000)
                .piecesAvailable(1)
                .build();



        UpdateItemRequest n = repo.updateWithExpression(a.withCost(8_815))
                .withCondition(it -> it.and(it.nameEqual("RedCar"),
                                            it.or(it.priceLessOrEqual(100),
                                                  it.piecesAvailableGreaterOrEqual(4))))
                                  .asUpdateItemRequest();

        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(
                n);

        assertThatThrownBy(f::get)
                .isInstanceOf(ConditionalCheckFailedException.class);

    }

    private static String getTableNamePrefix(){
        return UpdateRecordIT.class.getSimpleName();
    }

    private String getTableName(){
        return getTableNamePrefix() + UUID.randomUUID();
    }
}
