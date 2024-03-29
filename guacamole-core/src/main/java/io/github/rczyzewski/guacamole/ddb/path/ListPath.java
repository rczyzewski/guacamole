package io.github.rczyzewski.guacamole.ddb.path;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
public class ListPath<E, T> implements Path<E> {

  @Getter Path<E> parent;
  String selectedField;
  Function<Path<E>, T> provider;

  @Override
  public String serialize() {
    return Optional.ofNullable(parent)
        .map(Path::serialize)
        .map(it -> it + "." + selectedField)
        .orElse(selectedField);
  }

  @Override
  public Set<String> getPartsName() {
    Set<String> accumulator =
        Optional.ofNullable(parent).map(Path::getPartsName).orElse(new HashSet<>());
    accumulator.add(selectedField);
    return accumulator;
  }

  @Override
  public String serializeAsPartExpression(Map<String, String> shortCodeAccumulator) {

    String suffix = shortCodeAccumulator.get(selectedField);

    return Optional.ofNullable(parent)
        .map(it -> it.serializeAsPartExpression(shortCodeAccumulator))
        .map(it -> it + "." + suffix)
        .orElse(suffix);
  }

  public T at(int num) {
    Path<E> p = ListElement.<E>builder().parent(this).t(num).build();
    return provider.apply(p);
  }
}
