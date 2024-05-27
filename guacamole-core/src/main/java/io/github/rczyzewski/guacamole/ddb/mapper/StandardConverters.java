package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.experimental.UtilityClass;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@UtilityClass
public class StandardConverters {
  @UtilityClass
  public static class StringConverter {
    public static String fromAttribute(AttributeValue arg) {
      return arg.s();
    }

    public static AttributeValue toAttribute(String arg) {
      return AttributeValue.fromS(arg);
    }
  }

  @UtilityClass
  public static class IntegerConverter {

    public static Integer fromAttribute(AttributeValue arg) {
      return Integer.getInteger(arg.n());
    }

    public static AttributeValue toAttribute(Integer arg) {
      return AttributeValue.fromN(arg.toString());
    }
  }

  public static class LongConverter {

    public Long fromAttribute(AttributeValue arg) {
      return Long.getLong(arg.n());
    }

    public AttributeValue toAttribute(Long arg) {
      return AttributeValue.fromN(arg.toString());
    }
  }

  @UtilityClass
  public static class DoubleConverter {

    public static Double fromAttribute(AttributeValue arg) {
      return Double.parseDouble(arg.n());
    }

    public static AttributeValue toAttribute(Double arg) {
      return AttributeValue.fromN(arg.toString());
    }
  }

  @UtilityClass
  public static class FloatConverter {

    public static Float fromAttribute(AttributeValue arg) {
      return Float.parseFloat(arg.n());
    }

    public static AttributeValue toAttribute(Float arg) {
      return AttributeValue.fromN(arg.toString());
    }
  }

  @UtilityClass
  public static class AttributeConverter {

    public static AttributeValue fromAttribute(AttributeValue arg) {
      return arg;
    }

    public static AttributeValue toAttribute(AttributeValue arg) {
      return arg;
    }
  }
}
