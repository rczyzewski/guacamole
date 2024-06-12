#Annotation for defining attributes

## @DynamoDBTable

It is a class level annotation. Guacamole will generate a Repository code for a table, resepcting fields and indexes
declared in this class.
Example of usage:

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

## @DynamoDBDocument

It is a class level annotation. It allows to store documents inside other documents. This allows transforming an object
into Dynamo DB map structure, and revert it back.  
Guacamole will generate mapper/paths/conditions that are related to this class.

```java

@Value
@Builder
@With
@DynamoDBTable
public class Country{
    @DynamoDBHashKey
    String id;
    String name;
    Capital capital;

    @With
    @Value
    @Builder
    @DynamoDBDocument
    public static class Capital{
        String name;
        Long population;
    }
}

```

Defining it like above allows crating a filtering conditions for the object, like below:

```java
  CompletableFuture<ScanResponse> usingDynamoDBDocuments(){
    CountryRepository.Paths.Root path = new CountryRepository.Paths.Root();
    ScanRequest scanRequest =
            repo.scan()
                .condition(it -> it.compare(path.selectCapital().selectName(), EQUAL, "London"))
                .asScanItemRequest();
    return ddbClient.scan(scanRequest);
}

```

## @DynamoDBAttribute

This annotation changes the name of the attribute, that will be stored in DDB. By default, the name of the java property
is used.

```java

@Value
@Builder
@With
@DynamoDBTable
public class Country{
    @DynamoDBHashKey
    String id;
    String name;

    @DynamoDBAttribute(attributeName = "PRESIDENT")
    String headOfState;
}
```

## @DynamoDBHashKey

Defines the HashKey for the table.Applies only to Integers and Strings.
For the entity defined as follows:

```java

@Value
@Builder
@With
@DynamoDBTable
public class Country{
    @DynamoDBHashKey
    String id;
    String name;
}
```

The searching via primaryKey can be done like:

```java
  CompletableFuture<QueryResponse> queryingWithPrimaryKey(){
    QueryRequest queryRequest = repo.getIndexSelector().primary("EN").asQueryRequest();
    return ddbClient.query(queryRequest);
}

```

## @DynamoDBConverted

Defines a class that overrides conversion from/to dynamo. Let's first take a look at converter class:

```java

@UtilityClass
public class LocalDateTimeConverter implements GuacamoleConverter{
    public static final String DATETIME_PATTERN = "yyyy-MM-dd' 'HH:mm:ss.SSS";

    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_PATTERN).withZone(UTC);

    public static AttributeValue toValue(LocalDateTime ldt){
        return AttributeValue.fromS(DATETIME_FORMATTER.format(ldt));
    }

    public static LocalDateTime valueOf(AttributeValue attributeValue){
        return LocalDateTime.parse(attributeValue.s(), DATETIME_FORMATTER);
    }
}
```

It is important that `T valueOf(AttributeValue obj)` and `AttributeValue toValue(T obj)` are static.
Example usage:

```java

@Value
@Builder
@With
@DynamoDBTable
public class MyBooks{

    @DynamoDBHashKey
    String id;
    String title;
    List<String> authors;
    @DynamoDBConverted(converter = LocalDateTimeConverter.class)
    LocalDateTime published;
}
```

## @DynamoDBRangeKey

This annotation defines the main index for Dynamo DB table, It's not a required attribute. Applies only to Integers and Strings.
It is responsible for defining a local secondary index.
To give a better explanation of DynamoDB indexes, let's use example taken from DynamoDB 
[documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html)
The structure presented in AWS looks like follows: 
```java
@Value
@Builder
@With
@DynamoDBTable
public class PlayerRanking {

  @DynamoDBHashKey String userId;

  @DynamoDBIndexHashKey(globalSecondaryIndexNames = {"GameTitleIndex"})
  @DynamoDBRangeKey
  String gameTitle;

  @DynamoDBIndexRangeKey(globalSecondaryIndexNames = {"GameTitleIndex"})
  @DynamoDBAttribute(attributeName = "TopScore")
  Integer topScore;

  String topScoreDateTime;
  Integer wins;
  Integer loses;
}

```

Query that uses a local secondary index looks like follows: 
```jshelllanguage
     QueryRequest request = repo.getIndexSelector().primary("dd", q -> q.gameTitleEqual("ddd")).asQueryRequest();
```
## @DynamoDBLocalIndexRangeKey
It's usage is almost exactly the same as `@DynamoDBRangeKey`, except that the name of the index needs to be provided.
Take a look at example from `@DynamoDBRangeKey`.


## @DynamoDBIndexHashKey
It's usage is almost exactly the same as `@DynamoDBHashKey`, except that the name of the index needs to be provided.
Take a look at example from `@DynamoDBRangeKey`

## @DynamoDBIndexRangeKey
Defines a Local index, with the hash defined by @DynamoDBHashKey, and the range key with the defined attribute. Applies
only to Integers and Strings. It's usage is almost exactly the same a  `@DynamoDBRangeKey.java`. 
The difference is that the query would look like follows: 
```jshelllanguage
     QueryRequest request = repo.getIndexSelector().GameTitleIndex("Meteor Blaster").asQueryRequest();
```