package io.github.rczyzewski.testing_expressions;

import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.FixedExpression;
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@Testcontainers
public class PathCondtiionsTest{
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

    Employee employee = Employee
            .builder()
            .id("BAD0_F1CE")
            .name("HanSolo")
            .employees(Arrays.asList(Employee.builder().
                                             id("FED_C0FFEE")
                                             .name("Great and Might Chubacka")
                                             .build()))
            .department(Department.builder()
                                  .id("DEAD_BEEF")
                                  .location("Jakuu")
                                  .build())
            .build();

    EmployeeRepository repo  = new EmployeeRepository("FirstOrder");

    @BeforeEach
    @SneakyThrows
    void before(){
        ddbClient.createTable(repo.createTable()).get();
    }

    @Test
    @SneakyThrows
    void transactionExample(){
        ddbClient.putItem(repo.create(employee)).get();

        UpdateItemRequest request = repo.updateWithExpression(employee)
            .withCondition(it-> FixedExpression.<Employee>builder()
                                               .build())
            .asUpdateItemRequest();

        ddbClient.updateItem(request);
    }
}
