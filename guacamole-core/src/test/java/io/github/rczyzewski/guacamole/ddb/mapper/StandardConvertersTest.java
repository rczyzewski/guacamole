package io.github.rczyzewski.guacamole.ddb.mapper;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static org.assertj.core.api.Assertions.*;

class StandardConvertersTest {

  @Test
  void stringConverter() {
    AttributeValue attr = StandardConverters.StringConverter.toAttribute("foo");
    assertThat(attr).isEqualTo(AttributeValue.builder().s("foo").build());
    assertThat(StandardConverters.StringConverter.fromAttribute(attr)).isEqualTo("foo");
  }

  @Test
  void integerConverter() {
    AttributeValue attr = StandardConverters.IntegerConverter.toAttribute(12);
    assertThat(attr).isEqualTo(AttributeValue.builder().n("12").build());
    assertThat(StandardConverters.IntegerConverter.fromAttribute(attr)).isEqualTo(12);
  }

  @Test
  void longConverter() {
    AttributeValue attr = StandardConverters.LongConverter.toAttribute(12L);
    assertThat(attr).isEqualTo(AttributeValue.builder().n("12").build());
    assertThat(StandardConverters.LongConverter.fromAttribute(attr)).isEqualTo(12);
  }

  @Test
  void doubleConverter() {
    AttributeValue attr = StandardConverters.DoubleConverter.toAttribute(12.0);
    assertThat(attr).isEqualTo(AttributeValue.builder().n("12.0").build());
    assertThat(StandardConverters.DoubleConverter.fromAttribute(attr)).isEqualTo(12);
  }

  @Test
  void doubleConverterWithPi() {
    AttributeValue attr = StandardConverters.DoubleConverter.toAttribute(Math.PI);
    assertThat(attr).isEqualTo(AttributeValue.builder().n(Double.toString(Math.PI)).build());
    assertThat(StandardConverters.DoubleConverter.fromAttribute(attr)).isEqualTo(Math.PI);
  }

  @Test
  void floatConverter() {
    AttributeValue attr = StandardConverters.FloatConverter.toAttribute((float) Math.PI);
    assertThat(attr).isEqualTo(AttributeValue.builder().n(Float.toString((float) Math.PI)).build());
    assertThat(StandardConverters.FloatConverter.fromAttribute(attr)).isEqualTo((float) Math.PI);
  }

  @Test
  void attributeConverter() {
    AttributeValue arg = AttributeValue.builder().n(Float.toString((float) Math.PI)).build();
    AttributeValue converted = StandardConverters.AttributeConverter.toAttribute(arg);
    AttributeValue reconverted = StandardConverters.AttributeConverter.fromAttribute(converted);
    assertThat(arg).isEqualTo(reconverted);
    assertThat(converted).isEqualTo(reconverted);
  }
}
