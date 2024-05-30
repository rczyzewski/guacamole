package io.github.rczyzewski.guacamole.processor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import org.junit.jupiter.api.Test;

class TypoUtilsTest {

  @Test
  void uppercaseFirstLetter() {
    assertThat(TypoUtils.upperCaseFirstLetter("lower")).isEqualTo("Lower");
  }

  @Test
  void uppercaseFirstLetterEmpty() {
    String test = "";

    assertThat(TypoUtils.upperCaseFirstLetter(test)).isEmpty();
  }

  @Test
  void uppercaseFirstLetterNull() {
    assertThat(TypoUtils.upperCaseFirstLetter(null)).isNull();
  }

  @Test
  void toCamelCase() {

    assertThat(TypoUtils.toCamelCase("foo")).isEqualTo("foo");
    assertThat(TypoUtils.toCamelCase("secondRange")).isEqualTo("secondrange");
    assertThat(TypoUtils.toCamelCase("foo_bar")).isEqualTo("fooBar");
    assertThat(TypoUtils.toCamelCase("foo_bar_spam")).isEqualTo("fooBarSpam");
    assertThat(TypoUtils.toCamelCase("foo_bar-spam")).isEqualTo("fooBarSpam");
  }

  @Test
  void toClassName() {

    assertThat(TypoUtils.toClassName("ddddd")).isEqualTo("Ddddd");
    assertThat(TypoUtils.toClassName("foo_bar")).isEqualTo("FooBar");
    assertThat(TypoUtils.toClassName("foo_bar_spam")).isEqualTo("FooBarSpam");
    assertThat(TypoUtils.toClassName("foo_bar-spam")).isEqualTo("FooBarSpam");
  }
}
