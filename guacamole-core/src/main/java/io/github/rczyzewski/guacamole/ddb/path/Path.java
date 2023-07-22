package io.github.rczyzewski.guacamole.ddb.path;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Path<K>{
    String serialize();
    Path<K> getParent();
     default Set<String> getPartsName()
      {
          return Optional.ofNullable(getParent())
                  .map(Path::getPartsName)
                  .orElseGet(HashSet::new);
      }

    default String serializeAsPartExpression(Map<String, String> shortCodeAccumulator)
    {
        return Optional.ofNullable(getParent())
                .map(it -> it.serializeAsPartExpression(shortCodeAccumulator))
                .orElseThrow(RuntimeException::new);

    }
}

