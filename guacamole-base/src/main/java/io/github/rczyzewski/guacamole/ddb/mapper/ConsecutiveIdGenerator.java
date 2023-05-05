package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.StandardException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Value
@Builder
public class ConsecutiveIdGenerator implements Supplier<String>
{

    @Builder.Default
    AtomicInteger position = new AtomicInteger(0);

    @Builder.Default
    String base = "ABCDEFGHIJKLMNOPRSTUWXYZ";

    private String get(int position)
    {
        var length = base.length();
        int reminder = position % length;
        int new_position = position / length;
        var prefix = new_position == 0 ? "" : get(new_position - 1);
        return prefix + base.charAt(reminder);
    }

    public String get()
    {
        if (Objects.isNull(base) || base.isBlank())
            throw new UnsupportedArgumentException("Base can't be null or blank");
        return get(this.position.getAndIncrement());
    }

    @StandardException
    public static class UnsupportedArgumentException extends RuntimeException {}
}
