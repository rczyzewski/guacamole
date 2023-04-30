package io.github.rczyzewski.guacamole.examples.it;

import io.github.rczyzewski.guacamole.examples.TestHelperDynamoDB;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamo;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamoImpl;
import io.github.rczyzewski.guacamole.examples.model.ListWithObjectsFieldTable;
import io.github.rczyzewski.guacamole.examples.model.ListWithObjectsFieldTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.util.ArrayList;

@Slf4j
@Testcontainers
public class ListOfMappedObjectIT {


    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack =  new LocalStackContainer(localstackImage)
        .withServices(LocalStackContainer.Service.DYNAMODB)
        .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private  DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();
    private final RxDynamo rxDynamo  = new RxDynamoImpl(ddbClient);

    @Test
    void testStoringAList(){

        ListWithObjectsFieldTableRepository repo = new ListWithObjectsFieldTableRepository(rxDynamo, "randomTableName");

        rxDynamo.createTable(repo.createTable())
                .block();

        ArrayList<ListWithObjectsFieldTable.InnerObject> a = new ArrayList<>();
        a.add(ListWithObjectsFieldTable.InnerObject.builder().age("2").name("RCZ").build());

        ListWithObjectsFieldTable item = ListWithObjectsFieldTable.builder().uid("myUUID").payload(a).build();
        StepVerifier.create(repo.create(item)).expectNext(item).verifyComplete();

        StepVerifier.create(repo.getAll()).expectNext(item).verifyComplete();

    }


}
