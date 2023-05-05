package io.github.rczyzewski.guacamole.ddb.mapper;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
class ConsecutiveIdGeneratorTest
{
    @Test
    public void sunnyDay()
    {
        var idGenerator = ConsecutiveIdGenerator.builder().base("ABCD").build();
        var values = Stream.generate(idGenerator)
                           .limit(12)
                           .collect(Collectors.toList());

        Assertions.assertThat(values)
                  .isEqualTo(List.of("A", "B", "C", "D", "AA", "AB", "AC", "AD", "BA", "BB", "BC", "BD"));

    }

    @Test
    public void smallBase()
    {
        var idGenerator = ConsecutiveIdGenerator.builder().base("A").build();
        var values = Stream.generate(idGenerator)
                           .limit(1000)
                           .collect(Collectors.toList());

        Function<Integer, String> generateStringOfA =
            it -> IntStream.range(0, it).boxed().map(ignored -> "A").collect(Collectors.joining());

        IntStream.range(0, 1000)
                 .forEach(i -> Assertions.assertThat(values.get(i))
                                         .isEqualTo(generateStringOfA.apply(i+1)));

    }

    @Test
    public void emptyBase()
    {
        var idGenerator = ConsecutiveIdGenerator.builder().base("").build();
        Assertions.assertThatThrownBy(idGenerator::get).isInstanceOf(ConsecutiveIdGenerator.UnsupportedArgumentException.class);

    }
}