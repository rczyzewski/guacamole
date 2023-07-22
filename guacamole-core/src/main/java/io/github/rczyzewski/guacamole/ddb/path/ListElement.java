package io.github.rczyzewski.guacamole.ddb.path;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
@Builder
public class ListElement<E> implements Path<E>{

    @Getter
    @NonNull
    Path<E> parent;
    int t;

    @Override
    public String serialize(){
        return Optional.of(parent)
                       .map(Path::serialize)
                       .map(it -> it + String.format("[%s]", t))
                       .orElseThrow(() -> new IllegalArgumentException("DDB list element must be a part of the list") );
    }

    @Override
    public Set<String> getPartsName() {
        return parent.getPartsName();
    }

    @Override
    public String serializeAsPartExpression(Map<String, String> shortCodeAccumulator) {
        return Optional.of(parent)
                .map(it->it.serializeAsPartExpression(shortCodeAccumulator))
                .map(it -> it + String.format("[%s]", t))
                .orElseThrow(() -> new IllegalArgumentException("DDB list element must be a part of the list") );
    }
}
