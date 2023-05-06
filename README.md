# reactor-aws
utilities for accessing AWS dynamodb, s3 and sqs

Library is a collection patterns that were gathered while working with AWS.

The purpose reactor-ddb-om is to perform  DynamoDB operatioon with support of the compiler, lombok and the IDE.
After defining the model class like follows:

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

Your application code might look like:
```java
public class SmpleApp
{

    public static void main(String... args)
    {

        DynamoDbAsyncClient dynamoDBclient =
            DynamoDbAsyncClient.builder().region(Region.US_EAST_1).build();

        RxDynamo rxDynamo = new RxDynamoImpl(dynamoDBclient);
        CustomerRepository epo = new CustomerRepository(rxDynamo, "Customer");

        //default create tabe request - indexes already detected
        CreateTableRequest request = epo.createTable();
        //let's assume that wee need to customize it
        request.toBuilder().billingMode(BillingMode.PAY_PER_REQUEST).build();
        //creating the table in the cloud
        rxDynamo.createTable(request).block();

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

        //Scanning the table
        epo.getAll()
           .log("AllCustomers")
           .blockLast();

        //Getting all the customers with given id
        epo.primary()
           .keyFilter()
           .idEquals("id103")
           .end()
           .execute()
           .log("CustomerId103")
           .blockLast();

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

