package io.github.rczyzewski.guacamole.ddb.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Optional;
import java.util.function.Function;

interface Path<K>{
    String serialize();
}

@AllArgsConstructor
@Builder
class PrimitiveElement<E> implements Path<E>{
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

@AllArgsConstructor
@Builder
class TerminalElement<E> implements Path<E>{
    Path<E> parent;

    @Override
    public String serialize(){
        return parent.serialize();
    }
}

@AllArgsConstructor
@Builder
class ListElement<E> implements Path<E>{

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

@AllArgsConstructor
@Builder
class ListPath<E, T> implements Path<E>{

    Path<E> parent;
    String selectedField;
    Function<Path<E>, T> provider;

    @Override
    public String serialize(){
        return Optional.ofNullable(parent)
                       .map(Path::serialize)
                       .map(it -> it + ".")
                       .orElse(selectedField);
    }

    T at(int num){
        Path<E> p = ListElement.<E>builder().parent(this).t(num).build();
        return provider.apply(p);
    }

}
