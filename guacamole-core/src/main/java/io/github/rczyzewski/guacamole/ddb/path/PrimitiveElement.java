package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Optional;

@AllArgsConstructor
@Builder
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
}
