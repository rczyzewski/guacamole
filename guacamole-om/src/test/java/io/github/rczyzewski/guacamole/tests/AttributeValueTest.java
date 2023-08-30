package io.github.rczyzewski.guacamole.tests;


import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import java.nio.charset.Charset;
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

  private static final String TABLE_NAME = "Countries";

  private static final BooksRepository repo = new BooksRepository(TABLE_NAME);

  Books bb = Books.builder().isbn("I45678").customData(AttributeValue.fromS("ddddd")).build();
  Books vv = Books.builder().isbn("B12345").customData(AttributeValue.fromN("1985")).build();
  Books abc = Books.builder().isbn("B12345").customData(AttributeValue.fromN("1985")).build();
  Books dd = Books.builder()
          .isbn("H34505")
          .customData(AttributeValue.fromB(SdkBytes.fromString("hello", Charset.defaultCharset())))
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
  public void ddd() {

    repo.scan().condition(it -> it.isbnNotExists());
  }
}
