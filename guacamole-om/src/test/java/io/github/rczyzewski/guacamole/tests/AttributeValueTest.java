package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import java.time.LocalDateTime;
import java.util.Collections;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@Testcontainers
class AttributeValueTest {
  static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");

  @Container
  static LocalStackContainer localstack =
      new LocalStackContainer(localstackImage)
          .withServices(LocalStackContainer.Service.DYNAMODB)
          .withLogConsumer(new Slf4jLogConsumer(log));

  private static final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

  private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

  private static final String TABLE_NAME = "Books";

  private static final io.github.rczyzewski.guacamole.tests.BooksRepository repo =
      new io.github.rczyzewski.guacamole.tests.BooksRepository(TABLE_NAME);

  Books tomSawyer =
      Books.builder()
          .id("I45678")
          .authors(Collections.singletonList("Mark Twain"))
          .title("ddd")
          .notes(AttributeValue.fromS("some notes on the margin, made by author"))
          .build();

  Books huckFin =
      Books.builder()
          .id("B12345")
          .authors(Collections.singletonList("Mark Twain"))
          .title("")
          .notes(AttributeValue.fromL(Collections.singletonList(AttributeValue.fromN("1985"))))
          .build();

  Books catcherInTheRay =
      Books.builder()
          .id("B12345")
          .notes(AttributeValue.fromN("1985"))
          .published(LocalDateTime.now())
          .build();


  @BeforeAll
  @SneakyThrows
  static void beforeAll() {
    @Cleanup
    DynamoDbClient client = testHelperDynamoDB.getDdbClient();
    try {
      client.deleteTable(it -> it.tableName(repo.getTableName()));
    } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException ignored) {
    } finally {
      log.info("Table '{} deleted", repo.getTableName());
    }
    client.createTable(repo.createTable());
  }

  @Test
  @SneakyThrows
  void firstTest() {
    //TODO: add manny tests
    ScanRequest r =
        repo.scan()
            .condition(BooksRepository.LogicalExpressionBuilder::idNotExists)
            .asScanItemRequest();
    ScanResponse results = ddbClient.scan(r).get();
    assertThat(results.items()).isEmpty();
  }
}
