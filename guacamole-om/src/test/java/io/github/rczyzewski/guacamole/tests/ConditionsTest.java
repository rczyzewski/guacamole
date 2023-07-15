package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.EQUAL;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.GREATER;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.GREATER_OR_EQUAL;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.LESS_OR_EQUAL;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.NOT_EQUAL;
import static io.github.rczyzewski.guacamole.tests.CountryRepository.AllFields.FAMOUS_PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.of;

@Slf4j
@Testcontainers
class ConditionsTest {
    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

    private final static String TABLE_NAME = "Countries";

    private final CountryRepository repo = new CountryRepository(TABLE_NAME);
    private static final Country POLAND = Country.builder()
            .id("PL")
            .name("Poland")
            .fullName("Republic of Poland")
            .famousPerson("Stanislaw Lem")
            .area(312696F)
            .population(3803611)
            .density(122.0)
            .build();

    private static final Country UNITED_KINGDOM = Country.builder()
            .id("UK")
            .name("United Kingdom")
            .fullName("United Kingdom of Great Britain and Northern Ireland")
            .famousPerson("Brian May")
            .famousMusician("Brian May")
            .area(242495F)
            .population(6813848)
            .density(270.7)
            .build();

    @SneakyThrows
    @BeforeEach
    void beforeEach() {
        ddbClient.createTable(repo.createTable()).get();
        ddbClient.putItem(repo.create(POLAND)).get();
        ddbClient.putItem(repo.create(UNITED_KINGDOM)).get();
    }

    @AfterEach
    @SneakyThrows
    void afterEach() {
        ddbClient.deleteItem(repo.delete(POLAND)).get();
        ddbClient.deleteItem(repo.delete(UNITED_KINGDOM)).get();
    }

    interface CountryCondition extends Function<CountryRepository.LogicalExpressionBuilder, LogicalExpression<Country>> {
    }

    public static Arguments named(String s, CountryCondition c) {
        return Arguments.of(of(s, c));
    }
    private static Stream<Arguments> notMatchUnitedKingdom() {
        CountryRepository.Paths.CountryPath path = CountryRepository.Paths.CountryPath.builder().build();
        return Stream.of(
                named("when id is not defined",
                        it -> it.not(it.attributeExists(CountryRepository.AllFields.ID))),
                //String attribute exists - Enum approach
                named("has not a famous person",
                        it -> it.not(it.attributeExists(FAMOUS_PERSON))),
                //String to String comparison -> Path approach
                named("most famous person is not Brian May(not operator)",
                        it -> it.not( it.compare(path.selectFamousPerson(), EQUAL, "Brian May"))),
                named("most famous person is Brian May(NOT_EQUAL)",
                        it -> it.compare(path.selectFamousPerson(), NOT_EQUAL, "Brian May")),
                named("most famous person is less famous as Brian May(LESS)",
                        it -> it.compare(path.selectFamousPerson(), LogicalExpression.ComparisonOperator.LESS, "Brian May")),
                named("most famous person is less famous as Brian May(not)",
                        it -> it.not(it.compare(path.selectFamousPerson(), GREATER_OR_EQUAL, "Brian May"))),
                named("most famous person is no more famous than Brian May(GREATER)",
                        it -> it.compare(path.selectFamousPerson(), GREATER, "Brian May")),
                named("most famous person is no more famous than Brian May(not)",
                        it -> it.not(it.compare(path.selectFamousPerson(), LESS_OR_EQUAL, "Brian May"))),
                //TODO: whe there is not it.fullNameExists()
                //named("has a string property setup", (CountryCondition) it -> it.fullNameE ),
                //String attribute greater/equal/less
                named(  "when most famous person is not a Queen guitarist",
                        it -> it.famousPersonNotEqual("Brian May")),

                named(  "when most famous person is not a Queen guitarist(not)",
                        it -> it.not(it.famousPersonEqual("Brian May"))),

                named(  "when the most famous person is less famous than Brian",
                        it -> it.famousPersonLess( "Brian May")),

                named(  "when the most famous person is less famous than Brian(not)",
                        it -> it.not(it.famousPersonGreaterOrEqual( "Brian May"))),

                named("when famous person is less famous than Brian(less)",
                        it -> it.famousPersonLess("Brian May")),
                named("when famous person is less famous than Brian(NOT)",
                        it -> it.not(it.famousPersonGreaterOrEqual("Brian May"))),

                named("because he is  dictator(NOT)",
                        it -> it.not(it.famousPersonNotEqual( "Sacha Noam Baron Cohen"))),
                named("because he is dictator(EQUAL)",
                        it -> it.famousPersonEqual( "Sacha Noam Baron Cohen")),
                // the third way of making the same conditions
                named("when most famous person is a Queen guitarist",
                        it -> it.famousPersonNotEqual(CountryRepository.AllStrings.FAMOUS_MUSICIAN)),

                named("when most famous person is a Queen guitarist",
                        it -> it.not(it.famousPersonEqual(CountryRepository.AllStrings.FAMOUS_MUSICIAN))),

                named("when the most famous person is not at least as famous as Brian",
                        it -> it.famousPersonLess(CountryRepository.AllStrings.FAMOUS_MUSICIAN)),
                named("when the most famous person is not at least as famous as Brian",
                        it -> it.not(it.famousPersonGreaterOrEqual(CountryRepository.AllStrings.FAMOUS_MUSICIAN))),

                named("when famous person is no more famous than Brian",
                        it -> it.not(it.famousPersonLessOrEqual(CountryRepository.AllStrings.FAMOUS_MUSICIAN))),
                named("when famous person is no more famous than Brian",
                        it -> it.famousPersonGreater(CountryRepository.AllStrings.FAMOUS_MUSICIAN)),

                named("there should be someone more famous than the head of state(NOT)",
                        it -> it.not(it.famousPersonNotEqual(CountryRepository.AllStrings.NAME.name()))),
                named("there should be someone more famous than the head of state(EQUAL)",
                        it -> it.famousPersonEqual(CountryRepository.AllStrings.NAME.name()))

        );
    }

    private static Stream<Arguments> matchesUnitedKingdom() {
        CountryRepository.Paths.CountryPath path = CountryRepository.Paths.CountryPath.builder().build();
        return Stream.of(
                named("when id is defined",
                        it -> it.attributeExists(CountryRepository.AllFields.ID)),
                //TODO: why there is no  List & String attributes?
                //String attribute exists - Enum approach
                named("has a famous person",
                        it -> it.attributeExists(FAMOUS_PERSON)),
                //String to String comparison -> Path approach
                named("most famous person is Brian May",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, "Brian May")),
                named("most famous person is at least as famous as Brian May",
                        it -> it.compare(path.selectFamousPerson(), GREATER_OR_EQUAL, "Brian May")),
                named("most famous person is no more famous than Brian May",
                        it -> it.compare(path.selectFamousPerson(), LESS_OR_EQUAL, "Brian May")),
                named("because is different than dictator",
                        it -> it.compare(path.selectFamousPerson(), NOT_EQUAL, "Sacha Noam Baron Cohen")),
                //TODO: whe there is not it.fullNameExists()
                //named("has a string property setup", (CountryCondition) it -> it.fullNameE ),
                //String attribute greater/equal/less
                named(  "when most famous person is a Queen guitarist",
                        it -> it.famousPersonEqual("Brian May")),
                named(  "when the most famous person is at least as famous as Brian",
                        it -> it.famousPersonGreaterOrEqual( "Brian May")),
                named("when famous person is no more famous than Brian",
                        it -> it.famousPersonLessOrEqual("Brian May")),
                 named("because he is dictator", it -> it.famousPersonNotEqual( "Sacha Noam Baron Cohen")),
                // the third way of making the same conditions
                named("when most famous person is a Queen guitarist",
                        it -> it.famousPersonEqual(CountryRepository.AllStrings.FAMOUS_MUSICIAN)),
                named("when the most famous person is at least as famous as Brian",
                        it -> it.famousPersonGreaterOrEqual(CountryRepository.AllStrings.FAMOUS_MUSICIAN)),
                named("when famous person is no more famous than Brian",
                        it -> it.famousPersonLessOrEqual(CountryRepository.AllStrings.FAMOUS_MUSICIAN)),
                named("there should be someone more famous than the head of state",
                        it -> it.famousPersonNotEqual(CountryRepository.AllStrings.NAME.name()))
        );
    }

    @ParameterizedTest
    @MethodSource("matchesUnitedKingdom")
    @DisplayName("Update happens when conditions are matching")
    @SneakyThrows
    void updateWhenConditionsAreMatching(CountryCondition condition) {
        Country updatedUnitedKingdom = UNITED_KINGDOM.withHeadOfState("Charles III");
        UpdateItemRequest request =
                repo.updateWithExpression(updatedUnitedKingdom)
                        .withCondition(condition)
                        .asUpdateItemRequest();
        ddbClient.updateItem(request).get();
        ScanResponse response = ddbClient.scan(it -> it.tableName(TABLE_NAME)
                .build()).get();
        Map<String, Country> abc = response.items().stream().map(CountryRepository.COUNTRY::transform)
                .collect(Collectors.toMap(Country::getId, Function.identity()));
        Country poland = abc.get("PL");
        Country unitedKingdom = abc.get("UK");
        assertThat(unitedKingdom).isEqualTo(updatedUnitedKingdom);
        assertThat(poland).isEqualTo(POLAND);
    }
    @ParameterizedTest
    @MethodSource("notMatchUnitedKingdom")
    @DisplayName("Update does not happen when conditions are not matching")
    @SneakyThrows
    void doNotUpdateWhenConditionsAreNotMatching(CountryCondition condition) {
        Country updatedUnitedKingdom = UNITED_KINGDOM.withHeadOfState("Charles III");
        UpdateItemRequest request =
                repo.updateWithExpression(updatedUnitedKingdom)
                        .withCondition(condition)
                        .asUpdateItemRequest();

        assertThatThrownBy(() -> ddbClient.updateItem(request).get())
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

        ScanResponse response = ddbClient.scan(it -> it.tableName(TABLE_NAME)
                .build()).get();
        Map<String, Country> collected = response.items().stream().map(CountryRepository.COUNTRY::transform)
                .collect(Collectors.toMap(Country::getId, Function.identity()));
        Country poland = collected.get("PL");
        Country unitedKingdom = collected.get("UK");
        assertThat(unitedKingdom).isEqualTo(UNITED_KINGDOM);
        assertThat(poland).isEqualTo(POLAND);
    }
}
