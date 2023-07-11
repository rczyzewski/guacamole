package io.github.rczyzewski.testing_expressions;

import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;


@Slf4j
@Testcontainers
class PathConditionsTest{
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();
    private static final String TABLE_NAME="EmployeesTable";
    private final EmployeeRepository repo  = new EmployeeRepository(TABLE_NAME);

    private static final Employee han = Employee
            .builder()
            .id("A1")
            .name("HanSolo")
            .employees(Collections.singletonList(Employee.builder().
                                                         id("FED_C0FFEE")
                                                         .name("Great and Might Chubacka")
                                                         .build()))
            .department(Department.builder()
                                  .id("DEAD_BEEF")
                                  .location("Jakuu")
                                  .build())
            .build();
    private static final Employee padme = Employee
            .builder()
            .id("A2")
            .name("Padme Amidala")
            .department(Department.builder()
                                  .id("CAFE_BABE")
                                  .location("Jakuu")
                                  .build())
            .build();



    @SneakyThrows
    @BeforeEach
    void beforeEach(){
        ddbClient.createTable(repo.createTable()).get();
        ddbClient.putItem(repo.create(han)).get();
        ddbClient.putItem(repo.create(padme)).get();
    }
    @AfterEach
    @SneakyThrows
    void afterEach(){
        ddbClient.deleteItem(repo.delete(han)).get();
        ddbClient.deleteItem(repo.delete(padme)).get();
    }
    @Test
    @SneakyThrows
    void updateNumberConditionSuccess(){
        EmployeeRepository.Paths.EmployeePath path = EmployeeRepository.Paths.EmployeePath.builder().build();
        UpdateItemRequest request =
                repo.updateWithExpression(padme.withName("Matilda"))
                    .withCondition(it-> it.compare(path.selectName(), LogicalExpression.ComparisonOperator.EQUAL, "Padme Amidala"))
                    .asUpdateItemRequest();

        ddbClient.updateItem(request).get();
        ScanResponse dddd = ddbClient.scan(it -> it.tableName(TABLE_NAME)
                                                   .build()).get();

        Map<String, Employee> abc = dddd.items().stream().map(EmployeeRepository.EMPLOYEE::transform)
                                        .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Employee han = abc.get("A1");
        Employee padme = abc.get("A2");
        assertThat(padme.getName()).isEqualTo("Matilda");
        assertThat(han).isEqualTo(PathConditionsTest.han);
    }
}
