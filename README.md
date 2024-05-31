# Guacamole

Typesafe utilities for accessing AWS dynamodb


[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=rczyzewski_guacamole&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=rczyzewski_guacamole)

The purpose guacamole is to perform DynamoDB operation with support of the compiler, lombok and the IDE.

## Installation

```xml
<dependencies>
    <dependency>
        <groupId>io.github.rczyzewski</groupId>
        <artifactId>guacamole-om</artifactId>
        <version>0.1.1-RC2</version>
        <!-- the scope is provided, as this is required just to generate code accessing dynamodb -->
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.github.rczyzewski</groupId>
        <artifactId>guacamole-core</artifactId>
        <version>0.1.1-RC2</version>
    </dependency>
</dependencies>
```

The latest version always available in [here](https://mvnrepository.com/artifact/io.github.rczyzewski/guacamole-core).

## Definition of the entity 

```java
@Value
@With
@Builder
@DynamoDBTable
public class Customer{

    @DynamoDBHashKey
    String id;

    @DynamoDBRangeKey
    String name;

    String email;
    
    List<String> hobbies;

    @DynamoDBConverted(converter = InstantConverter.class)
    Instant registrationDate;
}
```
[Contribution guidelines for this project](docs/schema.md)

## Creation of the table

```jshelllanguage
    DynamoDbAsyncClient dynamoDBClient =
            DynamoDbAsyncClient.builder().region(Region.US_EAST_1)
                    .build();
    CustomerRepository repo = new CustomerRepository("io.github.rczyzewski.guacamole.tests.Customer");
    //default create table request - indexes already detected
    CreateTableRequest request = repo.createTable();
    //let's assume that wee need to customize it
    request.toBuilder().billingMode(BillingMode.PAY_PER_REQUEST)
            .build();
    //creating the table in the cloud
    rxDynamo.createTable(request).block();
```

## Inserting data

```jshelllanguage
    Instant instant = Instant.now();
    io.github.rczyzewski.guacamole.tests.Customer customer = io.github.rczyzewski.guacamole.tests.Customer.builder()
                                .email("sblue@noserver.com")
                                .id("id103")
                                .name("Susan Blue")
                                .regDate(instant)
                                .build();
    //Inserting new object into dynamoDB
    repo.create(customer)
        .map(it -> customer.withEmail("another@noserver.com"))
        //Executing update request for the same object
        .flatMap(epo::update)
        .block();
```

## Scann
method `getAll` is a 'syntax sugar' to get all the data.

```jshelllanguage
    CustomerRepository repo = new CustomerRepository(rxDynamo, "io.github.rczyzewski.guacamole.tests.Customer");
    //Scanning the table
    repo.getAll()
        .log("AllCustomers")
        .blockLast();
```

The same can be achieved like:

```jshelllanguage 
    //scanning primary key
    repo.primary()
       .execute()
       .log("AllCustomers")
       .blockLast();
```

## Query

To execute query and not a scan operation, need to provide keyFilter

```jshelllanguage
    //Getting all the customers with given id
    repo.primary()
       .keyFilter()
       .idEquals("id103")
       .end()
       .execute()
       .log("CustomerId103")
       .blockLast();
```

To provide 'post filtering conditions', need to provide filter() condition

```jshelllanguage
        repo.primary()
            .filter()
            .emailEquals("another@noserver.com")
            .execute()
            .log("CustomerWitEmai")
            .blockLast();
```

That is much shorter way, than using amazon client
directly: https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/examples-dynamodb.html

## Conditional updates

```jshelllanguage
    UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                              .withCondition(it -> it.and(it.nameEqual("RedCar"),
                                                          it.or(it.priceLessOrEqual(100),
                                                                it.piecesAvailableGreaterOrEqual(4))))
                              .asUpdateItemRequest();
    CompletableFuture <UpdateItemResponse> f = ddbClient.updateItem(n);
    assertThatThrownBy(f::get)
            .hasCauseInstanceOf(ConditionalCheckFailedException.class);
```

## Transactions

As Guacamole is purly concentrated on generating requests, those requests might be combined into transactions.

```jshelllanguage
    ddbClient.transactWriteItems(
        TransactWriteItemsRequest
                .builder()
                .transactItems(TransactWriteItem.builder()
                                                .update(Update.builder()
                                                .build())
                                       .put(Put.builder().build())
                                       .build())
                .build());

    repo = new ProductRepository(getTableName());
    rxDynamo.createTable(repo.createTable())
            .block();
    ddbClient.putItem(repo.create(product)).get();
```