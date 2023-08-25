package io.github.rczyzewski.guacamole.ddb.processor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TypoUtils {

  public static String toClassName(String input) {
    return TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(input));
  }

  public static String upperCaseFirstLetter(String input) {
    return Optional.ofNullable(input)
        .filter(i -> !i.isEmpty())
        .map(i -> i.substring(0, 1).toUpperCase(Locale.US) + i.substring(1))
        .orElse(input);
  }

  public static String lowerCaseFirstLetter(String input) {
    return Optional.ofNullable(input)
        .filter(i -> !i.isEmpty())
        .map(i -> i.substring(0, 1).toLowerCase(Locale.US) + i.substring(1))
        .orElse(input);
  }

  public static String toSnakeCase(String input) {

    final String REGEXP = "([a-z])([A-Z]+)";
    final String REPLACEMENT = "$1_$2";
    return input.replaceAll(REGEXP, REPLACEMENT).toUpperCase(Locale.US);
  }

  public static String toCamelCase(String input) {

    if (input == null) return null;
    return lowerCaseFirstLetter(
        Arrays.stream(input.split("[_\\-]"))
            // .flatMap( it -> Arrays.stream(it.split("-")))
            .filter(it -> !it.isEmpty())
            .map(it -> it.toLowerCase(Locale.US))
            .map(TypoUtils::upperCaseFirstLetter)
            .collect(Collectors.joining("")));
  }
}
