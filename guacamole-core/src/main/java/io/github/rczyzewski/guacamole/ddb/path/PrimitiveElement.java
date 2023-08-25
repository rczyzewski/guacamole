package io.github.rczyzewski.guacamole.ddb.path;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class PrimitiveElement<E, T> implements TypedPath<E, T> {
  Path<E> parent;
  String selectedElement;

  @Override
  public String serialize() {
    return Optional.ofNullable(parent)
        .map(Path::serialize)
        .map(it -> it + "." + selectedElement)
        .orElse(selectedElement);
  }

  @Override
  public Set<String> getPartsName() {
    Set<String> parentSet =
        Optional.ofNullable(parent).map(Path::getPartsName).orElseGet(HashSet::new);

    parentSet.add(selectedElement);

    return parentSet;
  }

  @Override
  public String serializeAsPartExpression(Map<String, String> shortCodeAccumulator) {
    String shortValue = shortCodeAccumulator.get(selectedElement);
    return Optional.ofNullable(parent)
        .map(it -> it.serializeAsPartExpression(shortCodeAccumulator))
        .map(it -> it + "." + shortValue)
        .orElse(shortValue);
  }
}
