package io.github.rczyzewski.guacamole;

import static java.time.ZoneOffset.UTC;

import io.github.rczyzewski.guacamole.ddb.datamodeling.GuacamoleConverter;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalDateTimeToLongConverter implements GuacamoleConverter {

  public static AttributeValue toValue(LocalDateTime ldt) {
    return AttributeValue.fromN(Long.toString(ldt.toEpochSecond(UTC)));
  }

  public static LocalDateTime valueOf(AttributeValue attributeValue) {
    return LocalDateTime.ofEpochSecond(Long.parseLong(attributeValue.n()), 0, UTC);
  }
}
