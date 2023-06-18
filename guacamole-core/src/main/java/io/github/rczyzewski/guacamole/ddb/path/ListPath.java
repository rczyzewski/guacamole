package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Optional;
import java.util.function.Function;

@AllArgsConstructor
@Builder
public class ListPath<E, T> implements Path<E>{

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

    public T at(int num){
        Path<E> p = ListElement.<E>builder().parent(this).t(num).build();
        return provider.apply(p);
    }

}
