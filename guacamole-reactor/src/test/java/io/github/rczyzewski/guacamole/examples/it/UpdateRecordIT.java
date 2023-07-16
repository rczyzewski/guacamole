package io.github.rczyzewski.guacamole.examples.it;

import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamo;
import io.github.rczyzewski.guacamole.examples.model.CompositePrimaryIndexTable;
import io.github.rczyzewski.guacamole.examples.model.CompositePrimaryIndexTableRepository;
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.util.UUID;

import static io.github.rczyzewski.guacamole.examples.model.CompositePrimaryIndexTableRepository.COMPOSITE_PRIMARY_INDEX_TABLE;

@Slf4j
@Testcontainers
class UpdateRecordIT{
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

    private final RxDynamo rxDynamo = new RxDynamo(ddbClient);

    @Test
    void simpleUpdate(){
        CompositePrimaryIndexTableRepository repo = new CompositePrimaryIndexTableRepository(getTableName());
        rxDynamo.createTable(repo.createTable())
                .block();
        CompositePrimaryIndexTable item =
                CompositePrimaryIndexTable.builder()
                                          .uid("someUID")
                                          .payload("ABC")
                                          .range("A")
                                          .fuzzyVal(322.0)
                                          .build();

        rxDynamo.save(repo.create(item)).block();
        StepVerifier.create(rxDynamo.search(repo.getAll())
                                    .map(COMPOSITE_PRIMARY_INDEX_TABLE::transform))
                    .expectNext(item).verifyComplete();
        rxDynamo.update(repo.updateWithExpression(item.withPayload(null)).asUpdateItemRequest()).block();
        StepVerifier.create(rxDynamo.search(repo.getAll())
                                    .map(COMPOSITE_PRIMARY_INDEX_TABLE::transform))
                    .expectNext(item).verifyComplete();
    }

    private static String getTableNamePrefix(){
        return UpdateRecordIT.class.getSimpleName();
    }

    private String getTableName(){
        return getTableNamePrefix() + UUID.randomUUID();
    }
}
