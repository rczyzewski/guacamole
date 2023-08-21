package io.github.rczyzewski.guacamole.tests.indexes;


import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
class IndexTest {
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private static final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

    private final static String TABLE_NAME = "Ranking";

    private final static PlayerRankingRepository repo = new PlayerRankingRepository(TABLE_NAME);

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        testHelperDynamoDB.getDdbAsyncClient().createTable(repo.createTable()).get();
    }

    @SneakyThrows
    @BeforeEach
    void beforeEach() {
        List<PlayerRanking> data = Arrays.asList(
                PlayerRanking.builder()
                        .userId("101")
                        .gameTitle("Galaxy Invaders")
                        .topScore(5842)
                        .topScoreDateTime("2015-09-15 17:24:31")
                        .wins(21)
                        .loses(72).build(),
                PlayerRanking.builder()
                        .userId("101")
                        .gameTitle("Meteor Blaster")
                        .topScore(1000)
                        .topScoreDateTime("2015-10-22 23:18:01")
                        .wins(12)
                        .loses(3).build(),
                PlayerRanking.builder()
                        .userId("102")
                        .gameTitle("Starship X")
                        .topScore(24)
                        .topScoreDateTime("2015-08-31 13:14:21")
                        .wins(21)
                        .loses(72).build(),
                PlayerRanking.builder()
                        .userId("102")
                        .gameTitle("Alien Adventure")
                        .topScore(24)
                        .topScoreDateTime("2015-07-12 11:07:56")
                        .wins(32)
                        .loses(192)
                        .build(),
                PlayerRanking.builder()
                        .userId("103")
                        .gameTitle("Attack Ships")
                        .topScore(3)
                        .topScoreDateTime("2015-10-19 01:13:24")
                        .wins(1)
                        .loses(8)
                        .build(),
                PlayerRanking.builder()
                        .userId("103")
                        .gameTitle("Galaxy Invaders")
                        .topScore(2317)
                        .topScoreDateTime("2015-09-11 11:06:53")
                        .wins(40)
                        .loses(3)
                        .build(),
                PlayerRanking.builder()
                        .userId("103")
                        .gameTitle("Meteor Blaster")
                        .topScore(723)
                        .topScoreDateTime("2015-10-19 01:13:24")
                        .wins(22)
                        .loses(12)
                        .build(),
                PlayerRanking.builder()
                        .userId("103")
                        .gameTitle("Starship X")
                        .topScore(24)
                        .topScoreDateTime("2015-07-11 06:53:00")
                        .wins(4)
                        .loses(19)
                        .build());

        for (PlayerRanking datum : data) {
            PutItemRequest putItemRequest = repo.create(datum);
            ddbClient.putItem(putItemRequest).get();
        }
    }
    @SneakyThrows
    private List<PlayerRanking> perfrom(QueryRequest request) {

        QueryResponse results = ddbClient.query(request).get();
        return results.items()
                .stream()
                .map(PlayerRankingRepository.PLAYER_RANKING::transform)
                .collect(Collectors.toList());


    }

    @Test
    @SneakyThrows
    void noResultsWhenUsingNonExistingHashKey() {
        QueryRequest request = repo.getIndexSelector()
                .GameTitleIndex("dd")
                .asQuerytemRequest();
        assertThat(perfrom(request)).isEmpty();

        request = repo.getIndexSelector()
                .primary("dd")
                .asQuerytemRequest();
        assertThat(perfrom(request)).isEmpty();

        request = repo.getIndexSelector()
                .primary("dd",g -> g.gameTitleEqual("ddd"))
                .asQuerytemRequest();
        assertThat(perfrom(request)).isEmpty();

        request = repo.getIndexSelector()
                .GameTitleIndex("dd",g -> g.topScoreLess(123))
                .asQuerytemRequest();
        assertThat(perfrom(request)).isEmpty();
    }

    @Test
    @SneakyThrows
    void emptyScanShouldReturnResults() {
        //TODO: when scanning without condition, the exception is thrown
        ScanRequest request = repo.scan().condition(PlayerRankingRepository.LogicalExpressionBuilder::userIdExists).asScanItemRequest();
        CompletableFuture<ScanResponse> results = ddbClient.scan(request);
        List<PlayerRanking> items = results.get().items()
                .stream()
                .map(PlayerRankingRepository.PLAYER_RANKING::transform)
                .collect(Collectors.toList());

        assertThat(items).isNotEmpty();

    }
    @Test
    @SneakyThrows
    void shouldRetriveResultsWhenScanningByExistingSecondaryKey() {
        QueryRequest request = repo.getIndexSelector().GameTitleIndex("Meteor Blaster").asQuerytemRequest();
        assertThat(perfrom(request)).isNotEmpty();

    }
    @Test
    @SneakyThrows
    void shouldBeAbleToObtainIndexSelector() {

        QueryRequest request = repo.getIndexSelector()
                .GameTitleIndex("Meteor Blaster", dd -> dd.topScoreLess(1000))
                .condition(it->it.winsBetween(2,222))
                .asQuerytemRequest();

        QueryRequest request2 = repo.getIndexSelector()
                .GameTitleIndex("Meteor Blaster", dd -> dd.topScoreLess(1000)).asQuerytemRequest();

        assertThat(perfrom(request)).isNotEmpty();

    }
    @Test
    @SneakyThrows
    void testingLocalIndex() {

        QueryRequest request = repo.getIndexSelector()
                .UserScoreIndex("101")
                .asQuerytemRequest();

        assertThat(perfrom(request)).isNotEmpty();

         request = repo.getIndexSelector()
                .UserScoreIndex("101", dd -> dd.topScoreBetween(1000, 1000000))
                .asQuerytemRequest();
        assertThat(perfrom(request)).isNotEmpty();
    }
}