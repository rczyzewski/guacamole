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

import java.util.Arrays;
import java.util.List;

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

        data.stream().map(repo::create).forEach(ddbClient::putItem);
    }


    @Test
     void intialTestOfTheQueryMethod() {
        //It's cool that it's compile now
        repo.query(it ->
        {
            it.GameTitleIndex("dd",
                    $ -> {
                        $.topScoreEqual(22);
                        return "ddd";
                    }
            );
            return "ddd";
        });
        assertThat("Works!").startsWithIgnoringCase("work");
    }
}