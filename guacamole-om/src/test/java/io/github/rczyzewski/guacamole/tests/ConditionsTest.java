package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.testhelper.TestHelperDynamoDB;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
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

import static io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator.AttributeType.LIST;
import static io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator.AttributeType.NUMBER;
import static io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator.AttributeType.STRING;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.EQUAL;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.GREATER;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.GREATER_OR_EQUAL;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.LESS;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.LESS_OR_EQUAL;
import static io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator.NOT_EQUAL;
import static io.github.rczyzewski.guacamole.tests.CountryRepository.AllFields.AREA;
import static io.github.rczyzewski.guacamole.tests.CountryRepository.AllFields.FAMOUS_MUSICIAN;
import static io.github.rczyzewski.guacamole.tests.CountryRepository.AllFields.FAMOUS_PERSON;
import static io.github.rczyzewski.guacamole.tests.CountryRepository.AllFields.HEAD_OF_STATE;
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

    private static final TestHelperDynamoDB testHelperDynamoDB = new TestHelperDynamoDB(localstack);

    private final DynamoDbAsyncClient ddbClient = testHelperDynamoDB.getDdbAsyncClient();

    private final static String TABLE_NAME = "Countries";

    private final static CountryRepository repo = new CountryRepository(TABLE_NAME);
   private final static String BRIAN_MAY = "Brian May";
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

    @BeforeAll
    @SneakyThrows
    static void beforeAll(){
        testHelperDynamoDB.getDdbAsyncClient().createTable( repo.createTable()).get();
    }

    @SneakyThrows
    @BeforeEach

    void beforeEach() {
        ddbClient.putItem(repo.create(POLAND)).get();
        ddbClient.putItem(repo.create(UNITED_KINGDOM)).get();
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
                named("has not a famous person",
                        it -> it.not(it.attributeExists(FAMOUS_PERSON))),
                named("has not a famous person - without ",
                        it -> it.attributeNotExists(FAMOUS_PERSON)),
                named("most famous person is not Brian May(not operator)",
                        it -> it.not( it.compare(path.selectFamousPerson(), EQUAL, "Brian May"))),
                named("most famous person is Brian May(NOT_EQUAL)",
                        it -> it.compare(path.selectFamousPerson(), NOT_EQUAL, "Brian May")),
                named("most famous person is less famous as Brian May(LESS)",
                        it -> it.compare(path.selectFamousPerson(), LESS, "Brian May")),
                named("most famous person is less famous as Brian May(not)",
                        it -> it.not(it.compare(path.selectFamousPerson(), GREATER_OR_EQUAL, "Brian May"))),
                named("most famous person is no more famous than Brian May(GREATER)",
                        it -> it.compare(path.selectFamousPerson(), GREATER, "Brian May")),
                named("most famous person is no more famous than Brian May(not)",
                        it -> it.not(it.compare(path.selectFamousPerson(), LESS_OR_EQUAL, "Brian May"))),
                named("has a string property setup",
                        CountryRepository.LogicalExpressionBuilder::headOfStateExists),
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
                        it -> it.famousPersonEqual(CountryRepository.AllStrings.NAME.name())),

                named("nothing compares to BrianMay(1)",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, path.selectName())),
                named("nothing compares to BrianMay(2)",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, path.selectPopulation())),
                named("nothing compares to BrianMay(2)",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, path.selectPopulation())),
                named("compares to BrianMay(including INTEGER)",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, path.selectPopulation())),
                named("compares to BrianMay(including DOUBLE)",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, path.selectDensity())),
                named("compares to BrianMay(including FLOAT)",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, path.selectArea())),
                named("compares to BrianMay(including not setup)",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, path.selectHeadOfState())),
                named("when famous person is Brian May, (written with AND)",
                        it -> it.and(it.famousPersonNotEqual("Brian May")) ),
                named(" when famous person is Brian May, (written with AND)",
                        it -> it.not(it.and(it.famousPersonEqual("Brian May"))) ),
                named("it's not Brian May and name is United Kingdom",
                        it -> it.and(it.famousPersonNotEqual("Brian May"), it.nameEqual("United Kingdom")) ),
                named("it's not Brian May and it's not United Kingdom",
                        it -> it.and(it.famousPersonNotEqual("Brian May"), it.nameNotEqual("United Kingdom")) ),
                named("it's not (Brian May and Untied Kingdom)",
                        it -> it.not(it.and(it.famousPersonEqual("Brian May"), it.nameEqual("United Kingdom")) )),
                named("it's Brian May and not UK and not(head of state exists)",
                        it -> it.and(
                                it.famousPersonEqual("Brian May"),
                                it.not(it.nameEqual("United Kingdom")),
                                it.not(it.attributeExists(CountryRepository.AllFields.HEAD_OF_STATE))
                        ) ),
                named(" when famous person is Brian May, (written with AND)",
                         it -> it.not(it.or(it.famousPersonEqual("Brian May"))) ),
                named("it's Brian May and  UK and head of state exists",
                        it -> it.or(
                                it.famousPersonNotEqual("Brian May"),
                                it.nameNotEqual("United Kingdom"),
                                it.attributeExists(CountryRepository.AllFields.HEAD_OF_STATE)
                        ) ),
                named("(it's Brian May and  UK) or head of state exists",
                        it -> it.or(
                                it.not(it.and(
                                        it.famousPersonEqual("Brian May"),
                                        it.nameEqual("United Kingdom"))),
                                it.attributeExists(CountryRepository.AllFields.HEAD_OF_STATE)
                        )),
                named("comparing String EQUAL Integer",
                        it -> it.compare(path.selectFamousPerson(), EQUAL,  1)),
                named("comparing String EQUAL Long",
                        it -> it.compare(path.selectFamousPerson(), EQUAL,  1L)),
                named("comparing String LESS Integer",
                        it -> it.compare(path.selectFamousPerson(), LESS,  1)),
                named("comparing String LESS Long",
                        it -> it.compare(path.selectFamousPerson(), LESS,  1L)),
                named("comparing String EQUAL Double",
                        it -> it.compare(path.selectFamousPerson(), EQUAL,  1D)),
                named("comparing String EQUAL Float",
                        it -> it.compare(path.selectFamousPerson(), EQUAL,  1F)),
                named("comparing String LESS Double",
                        it -> it.compare(path.selectFamousPerson(), LESS,  1D)),
                named("comparing String LESS Float",
                        it -> it.compare(path.selectFamousPerson(), LESS,  1F)),
                named("attributeExists - using logical expression builder",
                        CountryRepository.LogicalExpressionBuilder::headOfStateExists),
                named("attributeExists - using enum reference",
                        it-> it.attributeExists(CountryRepository.AllFields.HEAD_OF_STATE)),
                named("attributeDoNotExists - using logical expression builder",
                        CountryRepository.LogicalExpressionBuilder::famousMusicianNotExists),
                named("attributeDoNotExists - using negation and logical expression builder",
                        it-> it.not(it.attributeExists(FAMOUS_MUSICIAN))) ,
                named("attributeDoNotExists - using 'NotExist' method from logical expression builder",
                        it-> it.attributeNotExists(FAMOUS_MUSICIAN)),
                named("attributeDoNotExists - using paths",
                        it-> it.exists(path.selectHeadOfState())),
                    named("attributeExists - using paths",
                                it-> it.notExists(path.selectFamousPerson())),
                named("attributeType - STRING - using paths",
                        it-> it.isAttributeType( path.selectFamousPerson(), NUMBER) ),
                named("attributeType - NUMBER - using paths",
                        it-> it.isAttributeType( path.selectArea(), STRING) ),
                named("attributeType - NUMBER - using paths",
                        it -> it.and( it.isAttributeType(path.selectArea(), NUMBER),
                                it.isAttributeType(path.selectPopulation(), STRING))),
                named("attributeType - NUMBER - using Enums",
                        it -> it.and( it.isAttributeType(FAMOUS_MUSICIAN, NUMBER),
                                it.isAttributeType(AREA, NUMBER))),
                named("attributeType - NUMBER - using fluent expressions",
                        it -> it.and( it.isAttributeType(FAMOUS_MUSICIAN, NUMBER),
                                it.isAttributeType(AREA, NUMBER))),
                named("attributeType - STRING - using fluent expressions",
                        it -> it.famousMusicianIsAttributeType(NUMBER)),
                named("attributeType - NUMBER - using fluent expressions",
                        it -> it.areaIsAttributeType(STRING)),
                named("attributeType - NUMBER - using fluent expressions",
                        it -> it.areaIsAttributeType(LIST))
        );
    }

    private static Stream<Arguments> matchesUnitedKingdom() {
        CountryRepository.Paths.CountryPath path = CountryRepository.Paths.CountryPath.builder().build();
        return Stream.of(
                named("when id is defined",
                        it -> it.attributeExists(CountryRepository.AllFields.ID)),
                named("when some id is defined - with fluent query",
                        CountryRepository.LogicalExpressionBuilder::idExists),
                //String attribute exists - Enum approach
                named("has a famous person",
                        it -> it.attributeExists(FAMOUS_PERSON)),
                //String to String comparison -> Path approach
                named("most famous person is Brian May",
                        it -> it.compare(path.selectFamousPerson(), EQUAL, BRIAN_MAY )),
                named("most famous person is at least as famous as Brian May",
                        it -> it.compare(path.selectFamousPerson(), GREATER_OR_EQUAL, "Brian May")),
                named("most famous person is no more famous than Brian May",
                        it -> it.compare(path.selectFamousPerson(), LESS_OR_EQUAL, "Brian May")),
                named("because is different than dictator",
                        it -> it.compare(path.selectFamousPerson(), NOT_EQUAL, "Sacha Noam Baron Cohen")),
                named("has a string property setup",
                        CountryRepository.LogicalExpressionBuilder::fullNameExists),
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
                        it -> it.famousPersonNotEqual(CountryRepository.AllStrings.NAME.name())),
                named("when famous person is Brian May, (written with AND)",
                        it -> it.and(it.famousPersonEqual("Brian May")) ),
                named("when famous person is Brian May, and name is United Kingdom",
                        it -> it.and(it.famousPersonEqual("Brian May"), it.nameEqual("United Kingdom")) ),
                named("when famous person is Brian May, and name is United Kingdom",
                        it -> it.and(
                                it.famousPersonEqual("Brian May"),
                                it.nameEqual("United Kingdom"),
                                it.not(it.attributeExists(CountryRepository.AllFields.HEAD_OF_STATE))
                                )),
                named("when famous person is Brian May OR name is United Kingdom OR NOT Head of State exists",
                        it -> it.or(
                                it.famousPersonEqual("Brian May"),
                                it.nameEqual("United Kingdom"),
                                it.not(it.attributeExists(CountryRepository.AllFields.HEAD_OF_STATE))
                        )),
                named("when famous person is Brian May OR name is United Kingdom OR NOT Head of State exists",
                        it -> it.or( it.famousPersonEqual("Brian May"))),
                named("attributeExists",
                        CountryRepository.LogicalExpressionBuilder::famousPersonExists),
                named("attributeDoNotExists",
                        CountryRepository.LogicalExpressionBuilder::headOfStateNotExists),
                named("attributeExists - using generated enum",
                        it -> it.attributeExists(FAMOUS_MUSICIAN)),
                named("attributeDoNotExists - using generated enum",
                        it-> it.attributeNotExists(HEAD_OF_STATE)),
                named("attributeDoNotExists - using paths",
                        it-> it.notExists(path.selectHeadOfState())),
                named("attributeExists - using paths",
                            it-> it.exists(path.selectFamousPerson()) ),
                named("attributeType - STRING - using paths",
                        it-> it.isAttributeType( path.selectFamousPerson(), STRING) ),
                named("attributeType - NUMBER - using paths",
                        it-> it.isAttributeType( path.selectArea(), NUMBER) ),
                named("attributeType - NUMBER - using paths",
                        it -> it.or( it.isAttributeType(path.selectArea(), NUMBER),
                                it.isAttributeType(path.selectPopulation(), NUMBER))),
                named("attributeType - NUMBER - using Enums",
                        it -> it.or( it.isAttributeType(FAMOUS_MUSICIAN, NUMBER),
                                it.isAttributeType(AREA, NUMBER))),
                named("attributeType - NUMBER - using fluent expressions",
                        it -> it.or( it.isAttributeType(FAMOUS_MUSICIAN, NUMBER),
                                it.isAttributeType(AREA, NUMBER))),
                named("attributeType - NUMBER - using fluent expressions",
                        it -> it.famousMusicianIsAttributeType(STRING)),
                named("attributeType - NUMBER - using fluent expressions",
                        it -> it.areaIsAttributeType(NUMBER))
                /* TODO - failing: Duplicate key area
                named("attributeType - NUMBER - using paths - duplicated",
                        it -> it.or(
                                it.isAttributeType(path.selectArea(), ExpressionGenerator.AttributeType.NUMBER),
                                it.isAttributeType(path.selectArea(), ExpressionGenerator.AttributeType.NUMBER)
                        ))

                 */
        );
    }

    @ParameterizedTest
    @MethodSource("matchesUnitedKingdom")
    @DisplayName("Update happens when conditions are matching")
    @SneakyThrows
    void updateWhenConditionsAreMatching(CountryCondition condition) {
        Country updatedUnitedKingdom = UNITED_KINGDOM.withHeadOfState("Charles III");
        UpdateItemRequest request =
                repo.update(updatedUnitedKingdom)
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
                repo.update(updatedUnitedKingdom)
                        .withCondition(condition)
                        .asUpdateItemRequest();

        assertThatThrownBy(() -> ddbClient.updateItem(request).get())
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);
    }
}
