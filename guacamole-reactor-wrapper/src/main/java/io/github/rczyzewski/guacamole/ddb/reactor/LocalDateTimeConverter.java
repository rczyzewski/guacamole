package io.github.rczyzewski.guacamole.ddb.reactor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalDateTimeConverter
{
    public static final String DATETIME_PATTERN = "yyyy-MM-dd' 'HH:mm:ss.SSS";
    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_PATTERN).withZone(UTC);

    public static String toValue(LocalDateTime ldt)
    {
        return DATETIME_FORMATTER.format(ldt);
    }

    public static LocalDateTime valueOf(String ldt)
    {
        return LocalDateTime.parse(ldt, DATETIME_FORMATTER);
    }
}