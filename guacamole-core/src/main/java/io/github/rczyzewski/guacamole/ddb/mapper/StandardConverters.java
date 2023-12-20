package io.github.rczyzewski.guacamole.ddb.mapper;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class StandardConverters {
   interface StandardConverter<K> {
       K fromAttribute(AttributeValue arg) ;
       AttributeValue toAttribute(K arg) ;
   }
   public static class StringConverter  {
       static public String fromAttribute(AttributeValue arg) {
           return arg.s();
       }

       static public AttributeValue toAttribute(String arg) {
           return  AttributeValue.fromS(arg);
       }
   }
    public static class IntegerConverter {

        static public Integer fromAttribute(AttributeValue arg) {
            return Integer.getInteger(arg.n());
        }

        static public AttributeValue toAttribute(Integer arg) {
            return AttributeValue.fromN( arg.toString());
        }
    }
  public static class LongConverter {

      public Long fromAttribute(AttributeValue arg) {
          return null;
      }

      public AttributeValue toAttribute(Long arg) {
          return null;
      }
  }
    public static class DoubleConverter {

        static public Double fromAttribute(AttributeValue arg) {
            return null;
        }

        static public AttributeValue toAttribute(Double arg) {
            return null;
        }
    }
    public static class FloatConverter {

        static public Float fromAttribute(AttributeValue arg) {
            return null;
        }

        static public AttributeValue toAttribute(Float arg) {
            return null;
        }
    }
    public static class AttributeConverter{

        static public AttributeValue fromAttribute(AttributeValue arg) {
            return arg;
        }

        static public AttributeValue toAttribute(AttributeValue arg) {
            return arg;
        }
    }

}
