package io.github.rczyzewski.testing_expressions;

import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.FixedExpression;
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;


@Slf4j
@Testcontainers
class PathCondtiionsTest{
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();
    private final EmployeeRepository repo  = new EmployeeRepository("FirstOrder");

    private static final Employee employee = Employee
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



    /*
    @SneakyThrows
    @BeforeAll
    void dd(){
        ddbClient.putItem(repo.create(employee)).get();
        ddbClient.createTable(repo.createTable()).get();

        UpdateItemRequest request = repo.updateWithExpression(employee)
            .withCondition(it-> FixedExpression.<Employee>builder()
                                               .build())
            .asUpdateItemRequest();

        ddbClient.updateItem(request);
    }
    */
    @Test
    void howDDBWorks(){
        EmployeeRepository.Paths.EmployeePath.builder().build().serialize();
        repo.updateWithExpression(employee.withName("John"))
            .withCondition(it -> FixedExpression.<Employee>builder()
                                                .value(":ddd", AttributeValue.fromS("DDD"))
                                                .build());
        AttributeValue.fromNul(true);
        AttributeValue.fromL(Arrays.asList());
        AttributeValue.fromM(Collections.emptyMap());
        AttributeValue.fromNs(null);
        AttributeValue.fromB(SdkBytes.fromString("ddd", Charset.defaultCharset()));
    }


}
