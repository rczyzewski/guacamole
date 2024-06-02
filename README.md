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

## Repository setup

`Guacamole` for each class annotated with `@DynamoDBTable` generates a `Repository` class: the class will be located in
the same package as annotated class. Its name is concatenation of the original class name and `Repository`.
The generated class contains helper methods, to operate with dynamo db. The repository has a focus on two main areas:
generating requests to dynamoDB and serializing/deserializing objects coming from/to mongo.
Let's take a look how repository is initialized for the above class.

```
CustomerRepository repo = new CustomerRepository("customers");
```

Let's take a look, how this object, can help us with the creation of a table.

## Creation/Deletion of the table

In the sample blow we are defining DynamoDBClient - we might as well pick the asynchronous version.
Then we create the a `CreateTableRequest` using a `crateTable()` method.
Finally, we are invoking method of a client, to create a table, using `CreateTableRequest` as an argument.

```java
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

DynamoDbClient client = DynamoDbClient.create();
CreateTableRequest createTableRequest = repo.createTable();
CreateTableResponse creationTableResponse = client.createTable(createTableRequest);
```

Removal of the table is even easier:

```java
DeleteTableResponse deleteTableResponse = client.deleteTable(it -> it.tableName(repo.getTableName()));
```

The main difference, is that in case of removal of the table, repository is not providing a request object for the
dynamodDB client, as this request is less complex than `CreateTableRequest`. For the creation fo the table, `Guacamole`,
the `CreateTableRequest` must contain information about defined indexes.
As at this moment w already have a table crated, let's put some data into it.

## Inserting and updating the data

`Guacamole` helps generating `PutItemRequest` and `UpdateItemRequest` object. It is achieved by using method `create`,
provided by the repository class. In a very similar way the objects could be updated.

```java
Customer customer =
        Customer.builder()
                .id(uuid)
                .name("Joe")
                .address("Joe Street")
                .email("joe@email.com")
                .build();

PutItemRequest putItemRequest = repo.create(customer);
PutItemResponse putResponse = client.putItem(putItemRequest);

// Updating data
UpdateItemRequest update1 = repo.update(customer.withName("John Doe"))
                                .asUpdateItemRequest();
UpdateItemResponse updateItemResponse1 = client.updateItem(update1);
```

`DynamoDB` provides a conditional updates: it will update the document, only if the conditions is met.
More about conditions you will find in section related to querying and scanning.

```java
UpdateItemRequest update2 =
        repo.update(customer.withEmail("joe.doe@email.com"))
            .condition(it -> it.emailEqual("joe@email.com"))
            .asUpdateItemRequest();
UpdateItemResponse updateItemResponse2 = client.updateItem(update2);
```

## Scan

method `scan` is a 'syntax sugar' to get a ScanRequest object. For the simple case it might seem as an overkill,
however, as we will see soon, it allows to create conditions, that are 'safe' - possible for performing in dynamodb,
and relevant for the current data model.

```java
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

void scannAllData(CustomerRepository repo, DynamoDbClient client){
    ScanRequest scanRequest = repo.scan().asScanItemRequest();
    ScanResponseresponse = client.scan(scanRequest);
    for(Map<String, AttributeValue> item : response.items()){
        Customer retrivedCustomer = CustomerRepository.CUSTOMER.transform(item);
        log.info("retrieved all customers: {}", retrivedCustomer);
    }
}
```

I think the part ``CustomerRepository.CUSTOMER.transform(item);`` might catch attention. What is doing, is transforming
a map, into a `Customer` object. 
[//]: # (TODO: object mapper section in docs directory)


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