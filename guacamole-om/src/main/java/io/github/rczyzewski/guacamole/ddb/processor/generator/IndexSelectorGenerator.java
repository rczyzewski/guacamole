package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexSelectorGenerator {

        public TypeSpec createIndexSelectClass(
                ClassName externalClassName,
                List<IndexDescription> description) {
            TypeSpec.Builder ddd = TypeSpec.classBuilder(externalClassName.simpleName());
            for( IndexDescription index : description)
            {
                ddd.addMethod(MethodSpec.methodBuilder(Optional.ofNullable(index.getName()).orElse("primary")).build());
            }
            return ddd.build();
        }

}
