package io.github.rczyzewski.guacamole.ddb.path;

import java.util.HashSet;
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

   //  String serializeAsPartExpression(Map<String, String> shortCodeAccumulator);
    /* {
        //TODO: make a ROOT path in generated class, that can produce Paths, but it's not a path itself
        throw new RuntimeException("temporary fix");

    }
     */
}

