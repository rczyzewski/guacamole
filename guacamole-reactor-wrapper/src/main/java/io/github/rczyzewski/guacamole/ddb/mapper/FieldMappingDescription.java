package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.Value;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@Value
public class FieldMappingDescription<T>
{
    String ddbName;
    boolean keyValue;
    //TODO:  make specific classes
    BiFunction<T, AttributeValue, T> wither;
    Function<T, Optional<AttributeValue>> export;
}
