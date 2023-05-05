package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class UpdateExpression
{
    public interface AssignableValue
    {
        Map<String,AttributeValue> getValues();
        String serialize();
    }

    @Builder
    public static class  ConstantValue implements AssignableValue
    {

        @NotNull
        String valueCode;

        @NotNull
        AttributeValue attributeValue;
       public Map<String, AttributeValue>  getValues(){ return  Map.of( ":" + valueCode, attributeValue );  }
       public String serialize(){ return ":" + valueCode; }
    }
    /*
    public static class Plus {}
    public static class Minus{}
    public static class Function{}
    public static class Path{}
    */
    @Value
    @Builder
    public static class SetExpression
    {
        String fieldCode;
        String fieldDdbName;
        ConstantValue  value;
    }
    /*
    public static class AddExpression{}
    public static class RemoveExpression{}
    public static class DeleteExpression{}
     */
}
