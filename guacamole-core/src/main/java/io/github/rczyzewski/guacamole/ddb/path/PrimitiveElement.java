package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Optional;

@AllArgsConstructor
@Builder
public class PrimitiveElement<E> implements Path{
    //TODO: generate code in a way, that it allows PrimitiveElement<E> extend Paths<E>
    Path parent;
    String selectedElement;

    @Override
    public String serialize(){
        return Optional.ofNullable(parent)
                       .map(Path::serialize)
                       .map(it -> it + "." + selectedElement)
                       .orElse(selectedElement);
    }
}
