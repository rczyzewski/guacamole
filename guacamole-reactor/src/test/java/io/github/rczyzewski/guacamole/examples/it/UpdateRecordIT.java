package io.github.rczyzewski.guacamole.examples.it;

import io.github.rczyzewski.guacamole.ddb.MappedUpdateExpression;
import io.github.rczyzewski.guacamole.examples.TestHelperDynamoDB;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamo;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamoImpl;
import io.github.rczyzewski.guacamole.examples.model.CompositePrimaryIndexTable;
import io.github.rczyzewski.guacamole.examples.model.CompositePrimaryIndexTableRepository;
import io.github.rczyzewski.guacamole.examples.model.CompositePrimaryIndexTableRepository.AllFields;
import io.github.rczyzewski.guacamole.examples.model.CompositePrimaryIndexTableRepository.FieldAllNumbers;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

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

    private final RxDynamo rxDynamo = new RxDynamoImpl(ddbClient);

    @Test
    void veryFirstTestForConditionalUpdate(){
        CompositePrimaryIndexTableRepository repo = new CompositePrimaryIndexTableRepository(getTableName());
        rxDynamo.createTable(repo.createTable())
                .block();
        CompositePrimaryIndexTable a = CompositePrimaryIndexTable.builder()
                                                                 .uid("DDD")
                                                                 .range("ddd")
                                                                 .build();
        new CompositePrimaryIndexTableRepository("dd")
                .updateWithExpression(CompositePrimaryIndexTable.builder()
                                                                .uid("ddd").range("ABC").val(453)
                                                                .build())
                .withCondition(it -> it.and(it.rangeEqual("ABCDE"),
                                            it.or(it.valLessThan(4),
                                                  it.valLessThan(FieldAllNumbers.VAL))));

        MappedUpdateExpression<CompositePrimaryIndexTable, CompositePrimaryIndexTableRepository.LogicalExpressionBuilder> abc =
                new CompositePrimaryIndexTableRepository("dd")
                        .updateWithExpression(a)
                        .withCondition(it -> it.and(it.valLessThan(FieldAllNumbers.VAL),
                                                    it.or(it.valLessThan(13),
                                                          it.attributeExists(AllFields.PAYLOAD)
                                                         )));
        UpdateItemRequest cda = abc.serialize();

    }

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
        rxDynamo.update(repo.update(item.withPayload(null))).block();
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
