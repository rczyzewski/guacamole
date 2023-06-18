package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class TerminalElement<E> implements Path<E>{
    Path<E> parent;

    @Override
    public String serialize(){
        return parent.serialize();
    }
}
