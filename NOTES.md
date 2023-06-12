# guacamole - notes for developers

### Legacy conditional parameters

From [aws](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.html):
> ⚠ Important
> With the introduction of expression parameters, several older parameters have
> been deprecated. New applications should not use these legacy parameters, but
> should use expression parameters instead. For more information, see Using
> expressions in DynamoDB.
> 
> Additionally, DynamoDB does not allow mixing legacy conditional parameters and
> expression parameters in a single call. For example, calling the Query operation
> with AttributesToGet and ConditionExpression will result in an error.
>
##
Some attribute names are invalid, when using in expression, that is also incompatible:

```shell
> software.amazon.awssdk.services.dynamodb.model.DynamoDbException: 
> Invalid ConditionExpression: 
> Attribute name is a reserved keyword; reserved keyword: 
> range (Service: DynamoDb, Status Code: 400, Request ID: bbbc69dc-5bdb-4bfd-a147-6d1695722040)
```

### Debugging the AnnotationProcessor

One of the way to debug the build process is to follow instructions:
[link](https://medium.com/@joachim.beckers/debugging-an-annotation-processor-using-intellij-idea-in-2018-cde72758b78a)

### Working with localstack - aws emulator

Article presenting usage of the localstack:
[link](https://medium.com/geekculture/localstack-full-local-aws-stack-for-development-and-tests-in-docker-dd19ba2cecc2)
In short:

```shell
docker pull localstack/localstack
docker run -it -p 4566:4566 -p 4571:4571 localstack/localstack

```

Then check [health](http://localhost:4566/health)
Port 4566 is reposnsible for ddb service.

Admin for local ddb -[dynamo-db-admin](https://morioh.com/p/3b2d1a094050)

```shell
export DYNAMO_ENDPOINT=http://localhost:4566
cd ~/node_modules/dynamodb-admin/bin
./dynamodb-admin.js
```

It will expose a web interface: http://0.0.0.0:8001/

## processing annotations

Important link explaining how annotations are processed:
[link](https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation)

```java
class Example
{
    String getConverterClass(Element e)
    {
        String className = DynamoDBConverted.class.getName();
        return e.getAnnotationMirrors().stream().filter(
                    it -> it.getAnnotationType().toString().equals(className))
                .map(it -> it.getElementValues())
                .flatMap(it -> it.entrySet().stream())
                .filter(it -> it.getKey().equals("converter"))
                .map(it -> it.getValue())
                .map(it -> it.toString())
                .findFirst()
                .orElse(null);
    }
}
```
### Reserved Words in DynamoDB expressions
Reserved words are listed (here)[https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html]

As a consequence, expression can't look like this: 
```
(  name = :A ) and ( (  price <= :B ) or (  piecesAvailable >= :C ) )
```
because it produces the errors like: 
```
java.util.concurrent.ExecutionException:
software.amazon.awssdk.services.dynamodb.model.DynamoDbException:
Invalid ConditionExpression:
Attribute name is a reserved keyword; reserved keyword: name
(Service: DynamoDb, Status Code: 400, Request ID: 8352d1a1-0cdc-4dd4-b033-d03c083b599a)
```
In the above case it's sufficient, to move `name` into expressionAttributeNames. 


### Compatibility
Lombok do not support Java 21 - there are compilation issues when working with java 21
Today the last supported version is 20 - acording to lombok [changelog](https://projectlombok.org/changelog)
Compilation error: 
```shell
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile (default-compile) 
on project guacamole-core: 
  Fatal error compiling: 
  java.lang.NoSuchFieldError: 
  Class com.sun.tools.javac.tree.JCTree$JCImport does not have member field 'com.sun.tools.javac.tree.JCTree qualid'
``` 

Summary of testing:
145 - JDKs from sdkMan  
14 - failed because of java21 issues described above
1 - JDK was unable to download