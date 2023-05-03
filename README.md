# guacamole

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
Importatnty link explainig how annotations are processed:
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