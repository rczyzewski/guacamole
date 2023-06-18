# Guacamole

Typesafe utilities for accessing AWS dynamodb

The purpose guacamole is to perform  DynamoDB operatioon with support of the compiler, lombok and the IDE.

## Movie showing typing support
As the single picture can express more than thousands of words.

## Installation
```xml
<dependencies>
    <dependency>
        <groupId>io.github.rczyzewski</groupId>
        <artifactId>guacamole-om</artifactId>
        <version>0.1.1-RC1</version>
        <!-- the scope is provided, as this is required just to generate code accessing dynamodb -->
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.github.rczyzewski</groupId>
        <artifactId>guacamole-core</artifactId>
        <version>0.1.1-RC1</version>
    </dependency>
</dependencies>
```
To find the latest version check [mvnrepository](https://mvnrepository.com/artifact/io.github.rczyzewski/guacamole-core)

## Definition of the DDB table
```java
@Value
@With
@Builder
@DynamoDBTable
public class Customer {

    @DynamoDBHashKey
    String id;

    @DynamoDBRangeKey
    String name;

    String email;

    @DynamoDBConverted(converter = InstantConverter.class)
    Instant regDate;
}
```

###  creation of the table

```java
        DynamoDbAsyncClient dynamoDBclient =
            DynamoDbAsyncClient.builder().region(Region.US_EAST_1).build();
        CustomerRepository epo = new CustomerRepository("Customer");
        //default create tabe request - indexes already detected
        CreateTableRequest request = epo.createTable();
        //let's assume that wee need to customize it
        request.toBuilder().billingMode(BillingMode.PAY_PER_REQUEST).build();
        //creating the table in the cloud
        rxDynamo.createTable(request).block();
```

## filling with data
```
        LocalDate localDate = LocalDate.parse("2020-04-07");
        LocalDateTime localDateTime = localDate.atStartOfDay();
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

        Customer customer = Customer.builder()
                                    .email("sblue@noserver.com")
                                    .id("id103")
                                    .name("Susan Blue")
                                    .regDate(instant)
                                    .build();

        //Inserting new object into dynamoDB
        epo.create(customer)
           .map(it -> customer.withEmail("another@noserver.com"))
           //Executing update request for the same object
           .flatMap(epo::update)
           .block();
```


## querying and scanning 
Your application code might look like:
### scanning
method GetAll will is a 'syntax sugar' to get all the data.
```java
        CustomerRepository repo = new CustomerRepository(rxDynamo, "Customer");

        //Scanning the table
        epo.getAll()
           .log("AllCustomers")
           .blockLast();
```
The same can be achived like: 
``` java
        //scanning primary key
        epo.primary()
           .execute()  
           .log("AllCustomers")
           .blockLast();
```
### query
To execute query and not a scan operation, need to provide keyFilter
Those filters will be changed soon;-)
```java
        //Getting all the customers with given id
        epo.primary()
           .keyFilter()
           .idEquals("id103") 
           .end()
           .execute()
           .log("CustomerId103")
           .blockLast();
```
To provide 'post filtering conditions', need to provide filter() condition

```java
        epo.primary()
           .filter()
           .emailEquals("another@noserver.com")
           .end()
           .execute()
           .log("CustomerWitEmai")
           .blockLast();
    }
}

```
That is much shorter way, than using amazon client directly: https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/examples-dynamodb.html

## Conditional updates
Sometimes you want to be sure, that you are executiong operations only, when ceratain conditiaons are met.
It will be the killer feature! 
```java
    @Test
    void veryFirstTestForConditionalUpdate(){
        UpdateItemRequest n = repo.updateWithExpression(product.withCost(8_815))
                                  .withCondition(it -> it.and(it.nameEqual("RedCar"),
                                                              it.or(it.priceLessOrEqual(100),
                                                                    it.piecesAvailableGreaterOrEqual(4))))
                                  .asUpdateItemRequest();
        CompletableFuture<UpdateItemResponse> f = ddbClient.updateItem(n);
        assertThatThrownBy(f::get)
                .hasCauseInstanceOf(ConditionalCheckFailedException.class);

    }

```

### Transactions
As Guacamole is purly concentrated on generating requests, those requests might be combined into transactions.
```
    @BeforeEach
    @SneakyThrows
    void before(){
        ddbClient.transactWriteItems(
                TransactWriteItemsRequest
                        .builder()
                        .transactItems(TransactWriteItem.builder()
                                               .update(Update.builder().build())
                                               
                                               .put(Put.builder().build())
                                                        .build())
                        .build());
        repo = new ProductRepository(getTableName());
        rxDynamo.createTable(repo.createTable())
                .block();
        ddbClient.putItem( repo.create(product)).get();
    }
```

### Supported JDK and SDK
There is a prepared pipeline, that verifies, 
if code is generateed correcttly using a wide set of java compilers.
Thanks to [SDKman](https://sdkman.io/).
Same is happening for major versions of the SDKs