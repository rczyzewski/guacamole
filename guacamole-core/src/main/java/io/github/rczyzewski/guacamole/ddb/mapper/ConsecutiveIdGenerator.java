package io.github.rczyzewski.guacamole.ddb.mapper;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.experimental.StandardException;

@Builder
public class ConsecutiveIdGenerator implements Supplier<String> {

  @Builder.Default AtomicInteger position = new AtomicInteger(0);

  @Builder.Default String base = "ABCDEFGHIJKLMNOPRSTUWXYZ";

  private String get(int position) {
    int length = base.length();
    int reminder = position % length;
    int newPosition = position / length;
    String prefix = newPosition == 0 ? "" : get(newPosition - 1);
    return prefix + base.charAt(reminder);
  }

  public String get() {
    if (Objects.isNull(base) || base.trim().isEmpty())
      throw new UnsupportedArgumentException("Base can't be null or blank");
    return get(this.position.getAndIncrement());
  }

  @StandardException
  public static class UnsupportedArgumentException extends RuntimeException {}
}
