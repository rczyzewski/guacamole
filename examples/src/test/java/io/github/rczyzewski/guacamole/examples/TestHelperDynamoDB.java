package io.github.rczyzewski.guacamole.examples;

import lombok.AllArgsConstructor;
import org.testcontainers.containers.localstack.LocalStackContainer;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

@AllArgsConstructor
public class TestHelperDynamoDB
{

    private final LocalStackContainer localstack;

    public DynamoDbAsyncClient getDdbAsyncClient()
    {
        return DynamoDbAsyncClient.builder()
                                  .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                                  .credentialsProvider(
                                      StaticCredentialsProvider.create(
                                          AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                                      ))
                                  .region(Region.of(localstack.getRegion()))

                                  .build();
    }

    public DynamoDbClient getDdbClient()
    {
        return DynamoDbClient.builder()
                             .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                             .credentialsProvider(
                                 StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                                 ))
                             .region(Region.of(localstack.getRegion()))

                             .build();
    }

    public void ddbCleanup(String... tableNames)
    {
        Flux.fromArray(tableNames)
            .map(tableName -> getDdbClient().deleteTable(DeleteTableRequest.builder().tableName(tableName).build()))

            .blockLast();
    }

    public void ddbCleanup()
    {
        Flux.fromIterable(getDdbClient().listTables().tableNames())
            .map(tableName -> getDdbClient().deleteTable(DeleteTableRequest.builder().tableName(tableName).build()))
            .blockLast();
    }

}
