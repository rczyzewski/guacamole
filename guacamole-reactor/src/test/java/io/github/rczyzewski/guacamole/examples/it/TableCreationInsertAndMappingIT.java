package io.github.rczyzewski.guacamole.examples.it;

import io.github.rczyzewski.guacamole.ddb.reactor.RxDynamo;
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
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
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
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
        .withServices(LocalStackContainer.Service.DYNAMODB)
        .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();
    private final RxDynamo rxDynamo = new RxDynamo(ddbClient);

    @AfterAll
    static void cleanup()
    {
        //I guess, It might be reasonable to drop tables that have been created
    }

    @Test
    void simpleHashPrimaryIndex()
    {
        Hooks.onOperatorDebug();
        HashPrimaryIndexTableRepository repo = new HashPrimaryIndexTableRepository(getTableName());

        HashPrimaryIndexTable item = HashPrimaryIndexTable
            .builder()
            .uid("someUID")
            .payload("payload")
            .build();

        StepVerifier.create(
                        rxDynamo.createTable(repo.createTable())
                                .ignoreElement()
                                .thenReturn(item)
                                .flatMap(it -> rxDynamo.save(repo.create(it)))
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(HashPrimaryIndexTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(HashPrimaryIndexTableRepository.HASH_PRIMARY_INDEX_TABLE::transform)
                    )
                    .expectNext(item)
                    .verifyComplete();

        HashPrimaryIndexTable newItem = item.withPayload("newPayload");

        StepVerifier.create(
                        rxDynamo.update(repo.update(newItem)
                                        .asUpdateItemRequest())
                                .thenReturn(repo)
                                .map(HashPrimaryIndexTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(HashPrimaryIndexTableRepository.HASH_PRIMARY_INDEX_TABLE::transform)

                    )
                    .expectNext(newItem)
                    .verifyComplete();

        HashPrimaryIndexTable newNullItem = item.withPayload(null);

        StepVerifier.create(
                        rxDynamo.update(repo.update(newNullItem).asUpdateItemRequest())
                                .thenReturn(repo)
                                .map(HashPrimaryIndexTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(HashPrimaryIndexTableRepository.HASH_PRIMARY_INDEX_TABLE::transform)
                    )
                    .expectNext(newItem) // Must not override existing value with null
                    .verifyComplete();

    }

    @Test
    void localSecondaryIndexTableRepository()
    {

        LocalSecondaryIndexTableRepository repo = new LocalSecondaryIndexTableRepository(getTableName());

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
                                .flatMap(it -> rxDynamo.save(repo.create(it)))
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(LocalSecondaryIndexTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(LocalSecondaryIndexTableRepository.LOCAL_SECONDARY_INDEX_TABLE::transform)
                    )
                    .expectNext(item)
                    .verifyComplete();
    }

    @Test
    void globalHashIndexTableRepository()
    {

        GlobalHashIndexTableRepository repo = new GlobalHashIndexTableRepository(getTableName());

        GlobalHashIndexTable item = GlobalHashIndexTable
            .builder()
            .uid("someUID")
            .globalId("otherId")
            .payload("payload")
            .build();

        StepVerifier.create(
                        rxDynamo.createTable(repo.createTable()).ignoreElement()
                                .thenReturn(item)
                                .map(repo::create)
                                .flatMap(rxDynamo::save)
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(GlobalHashIndexTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(GlobalHashIndexTableRepository.GLOBAL_HASH_INDEX_TABLE::transform)
                    )
                    .expectNext(item)
                    .verifyComplete();
    }

    @Test
    void globalRangeIndexTableRepository()
    {

        GlobalRangeIndexTableRepository repo = new GlobalRangeIndexTableRepository(getTableName());

        GlobalRangeIndexTable item = GlobalRangeIndexTable
            .builder()
            .uid("someUID")
            .globalId("otherId")
            .globalRange("someRange")
            .payload("payload")
            .build();

        StepVerifier.create(
                        rxDynamo.createTable(repo.createTable())
                                .ignoreElement()
                                .thenReturn(item)
                                .map(repo::create)
                                .flatMap(rxDynamo::save)
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(GlobalRangeIndexTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(GlobalRangeIndexTableRepository.GLOBAL_RANGE_INDEX_TABLE::transform))
                    .expectNext(item)
                    .verifyComplete();
    }

    @Test
    void integerAsCompositeIndexRepository()
    {

        IntegerAsCompositeIndexTableRepository repo = new IntegerAsCompositeIndexTableRepository(getTableName());

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
                                .map(repo::create)
                                .flatMap(rxDynamo::save)
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(IntegerAsCompositeIndexTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(IntegerAsCompositeIndexTableRepository.INTEGER_AS_COMPOSITE_INDEX_TABLE::transform))
                    .expectNext(item)
                    .verifyComplete();
    }

    @Test
    void internalDocumentTableRepository()
    {

        InternalDocumentTableRepository repo = new InternalDocumentTableRepository(getTableName());

        InternalDocumentTable item = InternalDocumentTable
            .builder()
            .uid("uid")
            .payload("someString")
            .content(InternalDocumentTable.InternalDocumentContent.builder().payload("internalPayload").build())

            .build();

        StepVerifier.create(
                        rxDynamo.createTable(repo.createTable()).ignoreElement()
                                .thenReturn(item)
                                .map(repo::create)
                                .flatMap(rxDynamo::save)
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(InternalDocumentTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(InternalDocumentTableRepository.INTERNAL_DOCUMENT_TABLE::transform))
                    .expectNext(item)
                    .verifyComplete();
    }

    @Test
    void recursiveTableInsideATableRepository()
    {

        RecursiveTableRepository repo = new RecursiveTableRepository(getTableName());

        RecursiveTable inner = RecursiveTable
            .builder()
            .uid("uid")
            .payload("to understand recursion")
            .build();

        RecursiveTable item = inner.withData(inner).withPayload("you need to understand recursion");

        StepVerifier.create(
                        rxDynamo.createTable(repo.createTable()).ignoreElement()
                                .thenReturn(item)
                                .map(repo::create)
                            .flatMap(rxDynamo::save)
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(RecursiveTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(RecursiveTableRepository.RECURSIVE_TABLE::transform))
                    .expectNext(item)
                    .verifyComplete();
    }

    @Test
    void treeTableRepository()
    {

        TreeTableRepository repo = new TreeTableRepository(getTableName());

        TreeTable.TreeBranch rcz = TreeTable.TreeBranch.builder().payload("Rafal").build();
        TreeTable.TreeBranch gabor = TreeTable.TreeBranch.builder().payload("Gabor").build();
        TreeTable.TreeBranch gonzalo = TreeTable.TreeBranch.builder()
                                                           .payload("Gonzalo")
                                                           .subbranch(rcz)
                                                           .subbranch(gabor)
                                                           .build();
        TreeTable.TreeBranch json = TreeTable.TreeBranch.builder().payload("Json").subbranch(gonzalo).build();
        TreeTable.TreeBranch tania = TreeTable.TreeBranch.builder().payload("Tania").build();
        TreeTable.TreeBranch armando = TreeTable.TreeBranch.builder().payload("Armando").subbranch(tania).subbranch(
            json).build();

        TreeTable item = TreeTable
            .builder()
            .uid("uid")
            .content(armando)
            .build();

        StepVerifier.create(
                        rxDynamo.createTable(repo.createTable()).ignoreElement()
                                .thenReturn(item)
                                .map(repo::create)
                                .flatMap(rxDynamo::save)
                                .ignoreElement()
                                .thenReturn(repo)
                                .map(TreeTableRepository::getAll)
                                .flatMapMany(rxDynamo::search)
                                .map(TreeTableRepository.TREE_TABLE::transform))
                    .expectNext(item)
                    .verifyComplete();
    }

    private static String getTableNamePrefix()
    {
        return TableCreationInsertAndMappingIT.class.getSimpleName();
    }

    private String getTableName()
    {
        return getTableNamePrefix() + UUID.randomUUID();
    }
}
