package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Optional;

@AllArgsConstructor
@Builder
public class ListElement<E> implements Path<E>{

    Path<E> parent;
    int t;

    @Override
    public String serialize(){
        return Optional.ofNullable(parent)
                       .map(Path::serialize)
                       .map(it -> it + String.format("[%s]", t))
                       .orElse("DUPA");
    }

}
