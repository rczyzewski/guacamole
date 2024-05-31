# Guacamole

Typesafe utilities for accessing AWS dynamodb

![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=rczyzewski_guacamole&metric=alert_status)

The purpose guacamole is to perform DynamoDB operation with support of the compiler, lombok and the IDE.

## Installation

It is required, that when building a module containing classes annotated with `@DynamoDbTable`, there must
be `guacamole-om` available in the classpath. This way will allow AnnotationProcessor to create a Repository classes,
for each annotated class. It is sufficient to attach in a scope `provided` as, this is not required to use already
created classes.

```xml

<dependencies>
    <dependency>
        <groupId>io.github.rczyzewski</groupId>
        <artifactId>guacamole-om</artifactId>
        <version>0.1.1-RC3</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.github.rczyzewski</groupId>
        <artifactId>guacamole-core</artifactId>
        <version>0.1.1-RC3</version>
    </dependency>
</dependencies>
```

The latest version always available in [here](https://mvnrepository.com/artifact/io.github.rczyzewski/guacamole-core).

## Definition of the entity

Following snippets are take from this sample [file](guacamole-om/src/test/java/ExampleTest.java)
DynamoDB can be used without predefined structures: we can treat documents stored there as
a `Map<String, AttributeValue>`. The obvious benefit is that we don't need to define those properties upfront. However,
those properties need to be finally defined, to be able to use it. This way references to attributes will be appearing
close to a domain logic. If it's not defined in a single place, then those definitions will be scattered: probably by
using constant as attribute.

The oposite solution is to define entity, it's indexes in a single place, together with attached data about indexes and
attribute types. Thanks to this approach, we can generate a `Repository` classes, that can take over some boilerplate
code for commonly used scenarios. It simplifies refactoring a lot: popular IDE like intelliJ, can deal with refactoring
of this simple entity, and it's method usages. The generated `*Repository`classes, won't be refactored, however after
recompilation, usages that are no longer using existing filed or are not matching attribute types, will be reported
as errors.

This way compiler will tell us, that we are using things that are not valid for a given entity. In similar
situation `Map<String, AttributeValue>` won't report errors, that might appear during testing or even in production.

```java
@Value
@With
@Builder
@DynamoDBTable
class Customer{
    @DynamoDBHashKey
    String id;
    String name;
    String address;
    String email;
}
```

Let's focus for a while on annotations used

* `@Value`, `@With`, `@Builder` are [lombok](https://projectlombok.org/features/) annotations, that generate methods
  that are later used by `Guacamole`.
* `@DynamoDBTable` is an annotation, that will tell `Guacamole` to generate a repository for a given entity.
* `@DynamoDBHashKey` is an annotation that indicates where is the primary key for an entity.

[Detailed data about schema definition](docs/schema.md)

## Creation of the table

```jshelllanguage
    DynamoDbAsyncClient dynamoDBClient =
            DynamoDbAsyncClient.builder().region(Region.US_EAST_1)
                    .build();
    CustomerRepository repo = new CustomerRepository("Customer");
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
    Customer customer = Customer.builder()
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
    CustomerRepository repo = new CustomerRepository(rxDynamo, "Customer");
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
    CompletableFuture < UpdateItemResponse > f = ddbClient.updateItem(n);
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
                                       .put(Put.builder()
                                                    .build())
                                       .build())
                .build());

    repo = new ProductRepository(getTableName());
    rxDynamo.createTable(repo.createTable())
            .block();
    ddbClient.putItem(repo.create(product)).get();
```