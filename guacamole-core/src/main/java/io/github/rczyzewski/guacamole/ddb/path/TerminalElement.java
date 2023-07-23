package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Delegate;


@AllArgsConstructor
@Builder
public class TerminalElement<E> implements Path<E>{
    @Delegate
    Path<E> parent;

}
