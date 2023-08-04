package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.Map;

@UtilityClass
public class UpdateExpression
{

    @Builder
    public static class  ConstantValue
    {

        @NotNull
        String valueCode;

        @NotNull
        AttributeValue attributeValue;
       public Map<String, AttributeValue>  getValues(){ return Collections.singletonMap(":" + valueCode, attributeValue);  }
       public String serialize(){ return ":" + valueCode; }
    }
    @Value
    @Builder
    public static class SetExpression
    {
        String fieldCode;
        String fieldDdbName;
        ConstantValue  value;
    }
}
