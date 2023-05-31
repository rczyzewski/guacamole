package io.github.rczyzewski.guacamole.examples.it;

import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.examples.TestHelperDynamoDB;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamo;
import io.github.rczyzewski.guacamole.examples.shop.Product;
import io.github.rczyzewski.guacamole.examples.shop.ProductRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
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

    ProductRepository repo;

    Product product = Product
            .builder()
            .uid("2028JGG")
            .name("Millennium Falcon")
            .colors(Arrays.asList("red", "retired", "extremely", "dangerous"))
            .description("It's the property of Han Solo.")
            .price(8_000)
            .piecesAvailable(1)
            .build();

    @BeforeEach
    @SneakyThrows
    void before(){

        repo = new ProductRepository(getTableName());
        rxDynamo.createTable(repo.createTable())
                .block();
        ddbClient.putItem( repo.create(product)).get();


    }
    @Test
    void testingIfListContainsElement(){
        UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                                  .withCondition(it -> LogicalExpression.FixedExpression.
                                                  <Product>builder()
                                          .expression(" contains(tags, :value)   >= 5 ")
                                          .value(":value", AttributeValue.fromS("Jedi"))
                                          .build())
                                  .asUpdateItemRequest();
        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        assertThatThrownBy(f::get)
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

    }
    @Test
    void testingIfListContainsElementString(){
        UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                                  .withCondition(it -> LogicalExpression.FixedExpression
                                          .<Product>builder()
                                          .expression(" contains(description, :value)   >= 5 ")
                                          .value(":value", AttributeValue.fromS("red"))
                                          .build())
                                  .asUpdateItemRequest();
        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        assertThatThrownBy(f::get)
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

    }
    @Test
    void testingIfListContainsElementList(){
        UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                                  .withCondition(it -> LogicalExpression.FixedExpression
                                          .<Product>builder()
                                          .expression(" contains(color, :value)   >= 5 ")
                                          .value(":value", AttributeValue.fromS("red"))
                                                                                        .build())
                                  .asUpdateItemRequest();
        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        assertThatThrownBy(f::get)
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

    }
    @Test
    void testingSizeOfTheArrayExpression(){
        UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                                  .withCondition(it -> LogicalExpression.FixedExpression.<Product>builder()
                                                                 .expression(" size(color)   >= 5 ")
                                          .build())
                                  .asUpdateItemRequest();
        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        assertThatThrownBy(f::get)
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

    }

    @Test
    void veryFirstTestForConditionalUpdate(){
        UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                                  .withCondition(it -> it.and(it.nameEqual("RedCar"),
                                                              it.or(it.priceLessOrEqual(100),
                                                                    it.piecesAvailableGreaterOrEqual(4))))
                                  .asUpdateItemRequest();
        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        assertThatThrownBy(f::get)
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

    }
    @Test
    @SneakyThrows
    void checkUpdateWhenFieldIsDefined(){
        UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                                  .withCondition(it-> it.attributeExists(ProductRepository.AllFields.DESCRIPTION))
                                  .asUpdateItemRequest();

        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        UpdateItemResponse a = f.get();
        assertThat(a.consumedCapacity().capacityUnits()).isCloseTo(1.0, Offset.offset(0.001));

    }
    @Test
    @SneakyThrows
    void checkUpdateWhenFieldIsDefined2(){

        UpdateItemRequest n =
                repo.updateWithExpression(product.withCost(8_815))
                    .withCondition(it -> new LogicalExpression.AttributeExists<>(true, "test", "#TESTCODE"))
                    .asUpdateItemRequest();

        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        assertThatThrownBy(f::get)
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

    }

    private static String getTableNamePrefix(){
        return UpdateRecordIT.class.getSimpleName();
    }

    private String getTableName(){
        return getTableNamePrefix() + UUID.randomUUID();
    }
}
