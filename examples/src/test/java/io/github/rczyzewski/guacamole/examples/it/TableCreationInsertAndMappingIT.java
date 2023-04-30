package io.github.rczyzewski.guacamole.examples.it;

import io.github.rczyzewski.guacamole.examples.TestHelperDynamoDB;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamo;
import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamoImpl;
import io.github.rczyzewski.guacamole.examples.model.GlobalHashIndexTable;
import io.github.rczyzewski.guacamole.examples.model.GlobalHashIndexTableRepository;
import io.github.rczyzewski.guacamole.examples.model.GlobalRangeIndexTable;
import io.github.rczyzewski.guacamole.examples.model.GlobalRangeIndexTableRepository;
import io.github.rczyzewski.guacamole.examples.model.HashPrimaryIndexTable;
import io.github.rczyzewski.guacamole.examples.model.HashPrimaryIndexTableRepository;
import io.github.rczyzewski.guacamole.examples.model.IntegerAsCompositeIndexTable;
import io.github.rczyzewski.guacamole.examples.model.IntegerAsCompositeIndexTableRepository;
import io.github.rczyzewski.guacamole.examples.model.InternalDocumentTable;
import io.github.rczyzewski.guacamole.examples.model.InternalDocumentTableRepository;
import io.github.rczyzewski.guacamole.examples.model.LocalSecondaryIndexTable;
import io.github.rczyzewski.guacamole.examples.model.LocalSecondaryIndexTableRepository;
import io.github.rczyzewski.guacamole.examples.model.RecursiveTable;
import io.github.rczyzewski.guacamole.examples.model.RecursiveTableRepository;
import io.github.rczyzewski.guacamole.examples.model.TreeTable;
import io.github.rczyzewski.guacamole.examples.model.TreeTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Hooks;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import java.util.UUID;

@Slf4j
@Testcontainers
class TableCreationInsertAndMappingIT
{
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack =  new LocalStackContainer(localstackImage)
        .withServices(LocalStackContainer.Service.DYNAMODB)
        .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private  DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();
    private final RxDynamo rxDynamo = new RxDynamoImpl(ddbClient);

    @AfterAll
    static void cleanup()
    {
        //I guess, It might be reasonable to drop tablles that have been created
    }

    @Test
    void simpleHashPrimaryIndex()
    {
        Hooks.onOperatorDebug();
        HashPrimaryIndexTableRepository repo = new HashPrimaryIndexTableRepository(rxDynamo, getTableName());

        HashPrimaryIndexTable item = HashPrimaryIndexTable
            .builder()
            .uid("someUID")
            .payload("payload")
            .build();



        StepVerifier.create(
            rxDynamo.createTable(repo.createTable()).ignoreElement()
                .thenReturn(item)
                .flatMap(repo::create)
                .ignoreElement()
                .thenReturn(repo)
                .flatMapMany(HashPrimaryIndexTableRepository::getAll))
            .expectNext(item)
            .verifyComplete();

        HashPrimaryIndexTable newItem = item.withPayload("newPayload");

        StepVerifier.create(
            repo.update(newItem)
                .thenReturn(repo)
                .flatMapMany(HashPrimaryIndexTableRepository::getAll))
            .expectNext(newItem)
            .verifyComplete();

        HashPrimaryIndexTable newNullItem = item.withPayload(null);

        StepVerifier.create(
            repo.update(newNullItem)
                .thenReturn(repo)
                .flatMapMany(HashPrimaryIndexTableRepository::getAll))
            .expectNext(newItem) // Must not override existing value with null
            .verifyComplete();

    }

    @Test
    void localSecondaryIndexTableRepository()
    {

        LocalSecondaryIndexTableRepository repo = new LocalSecondaryIndexTableRepository(rxDynamo, getTableName());

        LocalSecondaryIndexTable item = LocalSecondaryIndexTable
            .builder()
            .uid("someUID")
            .range("A")
            .secondRange("B")
            .payload("payload")
            .build();

        StepVerifier.create(
            rxDynamo.createTable(repo.createTable()).ignoreElement()
                .thenReturn(item)
                .flatMap(repo::create)
                .ignoreElement()
                .thenReturn(repo)
                .flatMapMany(LocalSecondaryIndexTableRepository::getAll))
            .expectNext(item)
            .verifyComplete();
    }

    @Test
    void globalHashIndexTableRepository()
    {

        GlobalHashIndexTableRepository repo = new GlobalHashIndexTableRepository(rxDynamo, getTableName());

        GlobalHashIndexTable item = GlobalHashIndexTable
            .builder()
            .uid("someUID")
            .globalId("otherId")
            .payload("payload")
            .build();

        StepVerifier.create(
            rxDynamo.createTable(repo.createTable()).ignoreElement()
                .thenReturn(item)
                .flatMap(repo::create)
                .ignoreElement()
                .thenReturn(repo)
                .flatMapMany(GlobalHashIndexTableRepository::getAll))
            .expectNext(item)
            .verifyComplete();
    }

    @Test
    void globalRangeIndexTableRepository()
    {

        GlobalRangeIndexTableRepository repo = new GlobalRangeIndexTableRepository(rxDynamo, getTableName());

        GlobalRangeIndexTable item = GlobalRangeIndexTable
            .builder()
            .uid("someUID")
            .globalId("otherId")
            .globalRange("someRange")
            .payload("payload")
            .build();

        StepVerifier.create(
            rxDynamo.createTable(repo.createTable()).ignoreElement()
                .thenReturn(item)
                .flatMap(repo::create)
                .ignoreElement()
                .thenReturn(repo)
                .flatMapMany(GlobalRangeIndexTableRepository::getAll))
            .expectNext(item)
            .verifyComplete();
    }

    @Test
    void integerAsCompositeIndexRepository()
    {

        IntegerAsCompositeIndexTableRepository repo = new IntegerAsCompositeIndexTableRepository(rxDynamo, getTableName());

        IntegerAsCompositeIndexTable item = IntegerAsCompositeIndexTable
            .builder()
            .uid(12)
            .range(33)
            .payload("payload")
            .fuzzyVal(23.0)
            .val(12)
            .build();

        StepVerifier.create(
            rxDynamo.createTable(repo.createTable()).ignoreElement()
                .thenReturn(item)
                .flatMap(repo::create)
                .ignoreElement()
                .thenReturn(repo)
                .flatMapMany(IntegerAsCompositeIndexTableRepository::getAll))
            .expectNext(item)
            .verifyComplete();
    }

    @Test
    void internalDocumentTableRepository()
    {

        InternalDocumentTableRepository repo = new InternalDocumentTableRepository(rxDynamo, getTableName());

        InternalDocumentTable item = InternalDocumentTable
            .builder()
            .uid("uid")
            .payload("someString")
            .content(InternalDocumentTable.InternalDocumentContent.builder().payload("internalPayload").build())

            .build();

        StepVerifier.create(
            rxDynamo.createTable(repo.createTable()).ignoreElement()
                .thenReturn(item)
                .flatMap(repo::create)
                .ignoreElement()
                .thenReturn(repo)
                .flatMapMany(InternalDocumentTableRepository::getAll))
            .expectNext(item)
            .verifyComplete();
    }

    @Test
    void recursiveTableInsideATableRepository()
    {

        RecursiveTableRepository repo = new RecursiveTableRepository(rxDynamo, getTableName());

        RecursiveTable inner = RecursiveTable
                .builder()
                .uid("uid")
                .payload("to understand recursion")
                .build();


        RecursiveTable item = inner.withData(inner).withPayload("you need to understand recursion");

        StepVerifier.create(
                rxDynamo.createTable(repo.createTable()).ignoreElement()
                        .thenReturn(item)
                        .flatMap(repo::create)
                        .ignoreElement()
                        .thenReturn(repo)
                        .flatMapMany(RecursiveTableRepository::getAll))
                .expectNext(item)
                .verifyComplete();
    }


    @Test
    void treeTableRepository()
    {

      TreeTableRepository repo = new TreeTableRepository(rxDynamo, getTableName());

        TreeTable.TreeBranch rcz = TreeTable.TreeBranch.builder().payload("Rafal").build();
        TreeTable.TreeBranch gabor = TreeTable.TreeBranch.builder().payload("Gabor").build();
        TreeTable.TreeBranch gonzalo = TreeTable.TreeBranch.builder().payload("Gonzalo").subbranch(rcz).subbranch(gabor).build();
        TreeTable.TreeBranch json = TreeTable.TreeBranch.builder().payload("Json").subbranch(gonzalo).build();
        TreeTable.TreeBranch tania = TreeTable.TreeBranch.builder().payload("Tania").build();
        TreeTable.TreeBranch armando = TreeTable.TreeBranch.builder().payload("Armando").subbranch(tania).subbranch(json).build();



        TreeTable item = TreeTable
                .builder()
                .uid("uid")
                .content(armando)
                .build();

        StepVerifier.create(
                rxDynamo.createTable(repo.createTable()).ignoreElement()
                        .thenReturn(item)
                        .flatMap(repo::create)
                        .ignoreElement()
                        .thenReturn(repo)
                        .flatMapMany(TreeTableRepository::getAll))
                .expectNext(item)
                .verifyComplete();
    }


    private static String getTableNamePrefix(){
        return TableCreationInsertAndMappingIT.class.getSimpleName();
    }

    private String getTableName()
    {
        return getTableNamePrefix() + UUID.randomUUID();
    }
}
