package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

@UtilityClass
public class SchemaUtils {

  public static KeySchemaElement createKeySchemaElement(String attributeName, KeyType keyType) {
    return KeySchemaElement.builder().attributeName(attributeName).keyType(keyType).build();
  }
  public static AttributeDefinition createAttributeDefinition(String attributeName, String type) {
    return AttributeDefinition.builder().attributeName(attributeName).attributeType(type.toUpperCase()).build();
  }

  public static Projection createProjection(@NotNull ProjectionType projectionType) {
    return Projection.builder().projectionType(projectionType).build();
  }

  public static ProvisionedThroughput createThroughput(long writeCapacity, long readCapacity) {

    return ProvisionedThroughput.builder()
        .readCapacityUnits(readCapacity)
        .writeCapacityUnits(writeCapacity)
        .build();
  }
}
