package io.github.rczyzewski.guacamole.testhelper;

import lombok.AllArgsConstructor;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@AllArgsConstructor
public class TestHelperDynamoDB {

  private final LocalStackContainer localstack;

  public DynamoDbAsyncClient getDdbAsyncClient() {
    return DynamoDbAsyncClient.builder()
        .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
        .region(Region.of(localstack.getRegion()))
        .build();
  }
}
