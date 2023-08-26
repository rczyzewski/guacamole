package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.experimental.UtilityClass;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;

@UtilityClass
public class SchemaUtils {

  public static KeySchemaElement createKeySchemaElement(String attributeName, KeyType ddd) {
    return KeySchemaElement.builder().attributeName(attributeName).keyType(ddd).build();
  }
}
