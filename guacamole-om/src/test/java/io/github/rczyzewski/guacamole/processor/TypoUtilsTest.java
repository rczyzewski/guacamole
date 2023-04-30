package io.github.rczyzewski.guacamole.processor;

import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypoUtilsTest
{

    @Test
    void uppercaseFirstLetter()
    {
        String test = "lower";

        assertThat(TypoUtils.upperCaseFirstLetter(test)).isEqualTo("Lower");
    }

    @Test
    void uppercaseFirstLetterEmpty()
    {
        String test = "";

        assertThat(TypoUtils.upperCaseFirstLetter(test)).isEqualTo("");
    }

    @Test
    void uppercaseFirstLetterNull()
    {
        assertThat(TypoUtils.upperCaseFirstLetter(null)).isNull();
    }

    @Test
    void toCamelCase(){

        assertThat(TypoUtils.toCamelCase("ddddd")).isEqualTo("ddddd");
        assertThat(TypoUtils.toCamelCase("secondRange")).isEqualTo("secondrange");
        assertThat(TypoUtils.toCamelCase("foo_bar")).isEqualTo("fooBar");
        assertThat(TypoUtils.toCamelCase("foo_bar_spam")).isEqualTo("fooBarSpam");
        assertThat(TypoUtils.toCamelCase("foo_bar-spam")).isEqualTo("fooBarSpam");

    }

    @Test
    public void toClassName(){

        assertThat(TypoUtils.toClassName("ddddd")).isEqualTo("Ddddd");
        assertThat(TypoUtils.toClassName("foo_bar")).isEqualTo("FooBar");
        assertThat(TypoUtils.toClassName("foo_bar_spam")).isEqualTo("FooBarSpam");
        assertThat(TypoUtils.toClassName("foo_bar-spam")).isEqualTo("FooBarSpam");

    }


}