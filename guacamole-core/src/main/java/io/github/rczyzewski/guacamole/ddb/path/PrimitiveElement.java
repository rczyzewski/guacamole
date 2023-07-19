package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
@Builder
@Getter
public class PrimitiveElement<E> implements Path<E>{
    Path<E> parent;
    String selectedElement;

    @Override
    public String serialize(){
        return Optional.ofNullable(parent)
                       .map(Path::serialize)
                       .map(it -> it + "." + selectedElement)
                       .orElse(selectedElement);
    }

    @Override
    public Set<String> getPartsName() {
        Set<String> parentSet = Optional.ofNullable(parent)
                .map(Path::getPartsName)
                .orElseGet(HashSet::new);

       parentSet.add(selectedElement);

       return  parentSet;
    }

    /*
    @Override
    public String serializeAsPartExpression(Map shortCodeAccumulator) {
        Map<String, String> n = shortCodeAccumulator;
        String shortValue =(String) shortCodeAccumulator.get(selectedElement);
        return Optional.ofNullable(parent)
                .map(it -> it.serializeAsPartExpression(n))
                .map(it -> it + "." + shortValue)
                .orElse(shortValue);

    }
*/
}
