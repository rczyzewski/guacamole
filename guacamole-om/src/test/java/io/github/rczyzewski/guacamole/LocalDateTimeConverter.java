package io.github.rczyzewski.guacamole;


import io.github.rczyzewski.guacamole.ddb.datamodeling.GuacamoleConverter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalDateTimeConverter implements GuacamoleConverter
{
    public static final String DATETIME_PATTERN = "yyyy-MM-dd' 'HH:mm:ss.SSS";

    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_PATTERN).withZone(UTC);

    public static AttributeValue toValue(LocalDateTime ldt)
    {
       return AttributeValue.fromS(DATETIME_FORMATTER.format(ldt));
    }

    public static LocalDateTime valueOf(AttributeValue attributeValue)
    {
        return LocalDateTime.parse(attributeValue.s(), DATETIME_FORMATTER);
    }
}
