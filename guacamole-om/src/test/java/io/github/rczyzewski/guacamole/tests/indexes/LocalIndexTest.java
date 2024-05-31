package io.github.rczyzewski.guacamole.tests.indexes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Slf4j
@Testcontainers
class LocalIndexTest {
  static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");

  @Container
  static LocalStackContainer localstack =
      new LocalStackContainer(localstackImage)
          .withServices(LocalStackContainer.Service.DYNAMODB)
          .withLogConsumer(new Slf4jLogConsumer(log));

  private static final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

  private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

  private static final String TABLE_NAME = "ForumThread";

  private static final ForumThreadRepository repo = new ForumThreadRepository(TABLE_NAME);

  @BeforeAll
  @SneakyThrows
  static void beforeAll() {
    testHelperDynamoDB.getDdbAsyncClient().createTable(repo.createTable()).get();
  }

  @SneakyThrows
  @BeforeEach
  void beforeEach() {
    List<ForumThread> data =
        Arrays.asList(
            ForumThread.builder()
                .forumName("S3")
                .subject("aaa")
                .replies(12)
                .lastPostDateTime("2015-03-15 17:24:31")
                .build(),
            ForumThread.builder()
                .forumName("S3")
                .subject("bbb")
                .replies(3)
                .lastPostDateTime("2015-01-22 23:18:01")
                .build(),
            ForumThread.builder()
                .forumName("S3")
                .subject("ccc")
                .replies(4)
                .lastPostDateTime("2015-02-31 13:14:21")
                .build(),
            ForumThread.builder()
                .forumName("S3")
                .subject("ddd")
                .replies(9)
                .lastPostDateTime("2015-01-03 11:07:56")
                .build(),
            ForumThread.builder()
                .forumName("EC2")
                .subject("yyy")
                .replies(18)
                .lastPostDateTime("2015-02-12 11:07:56")
                .build(),
            ForumThread.builder()
                .forumName("EC2")
                .subject("zzz")
                .replies(0)
                .lastPostDateTime("2015-01-18 7:33:42")
                .build(),
            ForumThread.builder()
                .forumName("RDS")
                .subject("rrr")
                .replies(3)
                .lastPostDateTime("2015-01-19 01:13:24")
                .build(),
            ForumThread.builder()
                .forumName("RDS")
                .subject("sss")
                .replies(11)
                .lastPostDateTime("2015-03-11 06:53:00")
                .build(),
            ForumThread.builder()
                .forumName("RDS")
                .subject("ttt")
                .replies(5)
                .lastPostDateTime("2015-10-22 12:19:44")
                .build());

    for (ForumThread datum : data) {
      PutItemRequest putItemRequest = repo.create(datum);
      ddbClient.putItem(putItemRequest).get();
    }
  }

  @SneakyThrows
  private List<ForumThread> perform(QueryRequest request) {

    LiveMappingDescription<ForumThread> mapper = repo.getMapper();
    QueryResponse results = ddbClient.query(request).get();
    return results.items().stream().map(mapper::transform).collect(Collectors.toList());
  }

  @Test
  @SneakyThrows
  void testingLocalIndex() {

    QueryRequest request = repo.getIndexSelector().LastPostIndex("S3").asQueryRequest();
    assertThat(perform(request)).isNotEmpty();

    request =
        repo.getIndexSelector()
            .LastPostIndex(
                "S3",
                lastPostIndexKeyFilter ->
                    lastPostIndexKeyFilter.lastPostDateTimeEqual("2015-03-15 17:24:31"))
            .condition(it -> it.repliesGreater(1))
            .asQueryRequest();
    assertThat(perform(request)).isNotEmpty();
  }
}
