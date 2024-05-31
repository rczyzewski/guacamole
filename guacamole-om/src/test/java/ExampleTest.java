import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import java.util.UUID;

import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Slf4j
@Testcontainers
class ExampleTest {

  static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");

  @Container
  static LocalStackContainer localstack =
          new LocalStackContainer(localstackImage)
                  .withServices(LocalStackContainer.Service.DYNAMODB)
                  .withLogConsumer(new Slf4jLogConsumer(log));

  private final DynamoDbClient client = new TestHelperDynamoDB(localstack).getDdbClient();

  @BeforeEach
  void beforeEach() {
    try {
      client.deleteTable(it -> it.tableName(repo.getTableName()));
    } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException ignored) {

    } finally {
      log.info("Table '{} deleted", repo.getTableName());
    }
  }

  CustomerRepository repo = new CustomerRepository("customers");

  @Test
  void exampleTest() {

    String uuid = UUID.randomUUID().toString();

    // Creation/deletion of table
    client.createTable(repo.createTable());

    // Inserting data
    Customer customer =
        Customer.builder()
            .id(uuid)
            .name("Joe")
            .address("Joe Street")
            .email("joe@email.com")
            .build();

    client.putItem(repo.create(customer));

    // Updating data
    UpdateItemRequest update1 = repo.update(customer.withName("John Doe"))
                                    .asUpdateItemRequest();
    // Updating data - with condition
    client.updateItem(update1);
    UpdateItemRequest update2 =
        repo.update(customer.withEmail("joe.doe@email.com"))
            .condition(it -> it.emailEqual("joe@email.com"))
            .asUpdateItemRequest();

    client.updateItem(update2);

    // Scanning all data
    ScanRequest scanRequest = repo.scan().asScanItemRequest();

    client.scan(scanRequest).items().stream()
          .map(CustomerRepository.CUSTOMER::transform)
          .forEach(it -> log.info("retrieved all customers: {}", it));
    // Scanning data with condition
    ScanRequest scanRequest2 =
        repo.scan().condition(it -> it.emailBeginsWith("joe")).asScanItemRequest();

    client.scan(scanRequest2).items().stream()
        .map(CustomerRepository.CUSTOMER::transform)
        .forEach(it -> log.info("retrieved customer matching condition: {}", it));

    //Query - by index
    QueryRequest query =
            repo.getIndexSelector().primary("uuid").asQueryRequest();

    client.query(query).items().stream()
          .map(CustomerRepository.CUSTOMER::transform)
          .forEach(it -> log.info("retrieved customer: {}", it));

    //Query - with conditions
    QueryRequest query2 =
            repo.getIndexSelector().primary("uuid").asQueryRequest();

    client.query(query2).items().stream()
          .map(CustomerRepository.CUSTOMER::transform)
          .forEach(it -> log.info("retrieved customer matching uuid: {}", it));

    //Batch write
    Customer customer2 = customer.withId(UUID.randomUUID().toString());
    Customer customer3 = customer.withId(UUID.randomUUID().toString());
    client.batchWriteItem(it->it.requestItems(repo.asWriteRequest(customer2, customer3)));

    //Transactions - putting data
    Customer customer4 = customer.withId(UUID.randomUUID().toString()).withName("Alice");
    Customer customer5 = customer.withId(UUID.randomUUID().toString()).withName("Bob");
    client.transactWriteItems(
        TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .put(
                        Put.builder()
                            .tableName(repo.getTableName())
                            .item(repo.create(customer4).item())
                            .build())
                    .put(
                        Put.builder()
                            .tableName(repo.getTableName())
                            .item(repo.create(customer5).item())
                            .build())
                    .build())
            .build());

    //Transactions - updating data
    UpdateItemRequest update4 =
            repo.update(customer.withAddress("joe.doe@email.com"))
                .condition(it -> it.addressEqual("Joe Street"))
                        .asUpdateItemRequest();

    client.transactWriteItems(
        TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .update(
                        Update.builder()
                            .key(update4.key())
                            .tableName(repo.getTableName())
                            .conditionExpression(update4.conditionExpression())
                            .expressionAttributeValues(update4.expressionAttributeValues())
                            .expressionAttributeNames(update4.expressionAttributeNames())
                            .updateExpression(update4.updateExpression())
                            .build())
                    .build())
            .build());
  }
}

@Value
@With
@Builder
@DynamoDBTable
class Customer {
  @DynamoDBHashKey String id;
  String name;
  String address;
  String email;
}
