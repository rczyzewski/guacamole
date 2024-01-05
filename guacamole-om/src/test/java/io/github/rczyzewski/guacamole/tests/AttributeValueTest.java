package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import static java.time.ZoneOffset.UTC;
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
          .notes(AttributeValue.fromS("ddddd"))
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
    testHelperDynamoDB.getDdbAsyncClient().createTable(repo.createTable()).get();
  }

  @SneakyThrows
  @BeforeEach
  void beforeEach() {}

  @Test
  @SneakyThrows
  void firstTest() {
    Instant a = Clock.fixed(Instant.ofEpochSecond(10000), UTC).instant();

    // Instant.ofEpochSecond()
    ScanRequest r = repo.scan().condition(BooksRepository.LogicalExpressionBuilder::idNotExists).asScanItemRequest();
    ScanResponse results = ddbClient.scan(r).get();
    assertThat(results.items() ).isEmpty();
  }
  @Test
  void pathTest(){
    BooksRepository.Paths.Root pathCreator = new BooksRepository.Paths.Root();
    pathCreator.selectTitles().at(1);
    pathCreator.selectFullAuthorNames().at(1).at(1);
    pathCreator.selectReferences().at(1);
  }
}
