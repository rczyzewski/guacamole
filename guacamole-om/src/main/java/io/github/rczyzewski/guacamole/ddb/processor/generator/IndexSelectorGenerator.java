package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.*;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class IndexSelectorGenerator {

    public TypeSpec createIndexSelectClass(
            ClassName externalClassName,
            ClassName baseBean,
            List<IndexDescription> description) {
        TypeSpec.Builder indexSelectorBuilder = TypeSpec.classBuilder(externalClassName.simpleName());
        for (IndexDescription index : description) {
            indexSelectorBuilder.addMethod(this.createMethod(index));

            if (index.getRangeField() != null) {
                ClassName helperSecondaryIndex = externalClassName.nestedClass(index.getRangeField().getName());
                indexSelectorBuilder.addType(createSecondryIndexSelectorHelper(helperSecondaryIndex, baseBean, index));
                indexSelectorBuilder.addMethod(createMethod2(index, helperSecondaryIndex));
            }
        }

        return indexSelectorBuilder.build();
    }

    private MethodSpec createMethod(IndexDescription index) {

        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder(Optional.ofNullable(index.getName())
                        .orElse("primary"));
        methodBuilder.addParameter(ParameterSpec.builder(index.getHashField().getClass(), "hash").build());
        return methodBuilder.build();
    }

    private TypeSpec createSecondryIndexSelectorHelper(ClassName fullName, ClassName baseBean, IndexDescription index) {

        TypeSpec.Builder indexSelectorBuilder = TypeSpec.classBuilder(fullName.simpleName());
        LogicalExpressionBuilderGenerator g = new LogicalExpressionBuilderGenerator();

        Stream.of(ComparisonOperator.LESS,
                        ComparisonOperator.EQUAL,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        ComparisonOperator.LESS_OR_EQUAL)
                .map(it -> g.createFilterConditionsComparedToValue(baseBean, index.getRangeField(), it))
                .forEach(indexSelectorBuilder::addMethod);

        return indexSelectorBuilder.build();
    }

    private MethodSpec createMethod2(IndexDescription index, ClassName helperSecondaryIndex) {

        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder(Optional.ofNullable(index.getName())
                        .orElse("_primary"));
        TypeName argTup = ClassName.bestGuess(index.getHashField().getTypeName());
        methodBuilder.addParameter(ParameterSpec.builder(argTup, "hash").build());

        ParameterizedTypeName functionSignature = ParameterizedTypeName.get(ClassName.get(Function.class), helperSecondaryIndex , ClassName.get(String.class));
        methodBuilder.addParameter(ParameterSpec.builder(functionSignature, "range").build());
        return methodBuilder.build();
    }
}
