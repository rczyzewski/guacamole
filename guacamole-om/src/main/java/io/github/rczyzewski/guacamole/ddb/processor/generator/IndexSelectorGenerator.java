package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.*;
import io.github.rczyzewski.guacamole.ddb.MappedQueryExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class IndexSelectorGenerator {

    public TypeSpec createIndexSelectClass(
            ClassName externalClassName,
            ClassName baseBean,
            ClassName logicalExpressionBuilder,
            List<IndexDescription> description) {

        TypeSpec.Builder indexSelectorBuilder = TypeSpec.classBuilder(externalClassName.simpleName());

        for (IndexDescription index : description) {


            indexSelectorBuilder.addMethod(this.createMethod(index,baseBean, logicalExpressionBuilder));

            if (index.getRangeField() != null) {
                ClassName helperSecondaryIndex = externalClassName.nestedClass(index.getName() + index.getRangeField().getName());
                indexSelectorBuilder.addType(createSecondryIndexSelectorHelper(helperSecondaryIndex, baseBean, index));
                indexSelectorBuilder.addMethod(createMethod2(index, baseBean,  logicalExpressionBuilder, helperSecondaryIndex));
            }
        }
        return indexSelectorBuilder.build();
    }

    private MethodSpec createMethod(IndexDescription index, ClassName baseBean, ClassName generator) {
        ParameterizedTypeName rt = ParameterizedTypeName.get(ClassName.get(MappedQueryExpression.class), baseBean, generator);
        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder(Optional.ofNullable(index.getName())
                        .orElse("primary"));
        methodBuilder.addParameter(ParameterSpec.builder(ClassName.bestGuess(index.getHashField().getTypeName()), "hash").build())
                .addCode(CodeBlock.builder()
                        .add("$T tmp1 = $T.fromS(hash);\n", AttributeValue.class, AttributeValue.class)
                        .add("Path<$T> path = (new Paths.Root()).select$L()\n;", baseBean, TypoUtils.upperCaseFirstLetter(index.getHashField().getName()))
                        .add("LogicalExpression<$T> key = new LogicalExpression.ComparisonToValue<>(path , LogicalExpression.ComparisonOperator.EQUAL, tmp1 );\n", baseBean)
                        .add("$T ddd  =  new $T<>(new $T(), $S,  null, null, null, key); \n", rt, rt.rawType, rt.typeArguments.get(1), index.getName())
                        .build())
                .addCode("return ddd;")
                .returns(rt);

        return methodBuilder.build();
    }

    private TypeSpec createSecondryIndexSelectorHelper(ClassName fullName, ClassName baseBean, IndexDescription index) {

        TypeSpec.Builder indexSelectorBuilder = TypeSpec.classBuilder(fullName.simpleName());
        LogicalExpressionBuilderGenerator g = new LogicalExpressionBuilderGenerator();

        Stream.of(
                        ComparisonOperator.EQUAL,
                 ComparisonOperator.LESS,
                       ComparisonOperator.LESS_OR_EQUAL ,
              ComparisonOperator.GREATER,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        ComparisonOperator.BETWEEN ,
                        ComparisonOperator.BEGINS_WITH
                )
                .map(it -> g.createFilterConditionsComparedToValue(baseBean, index.getRangeField(), it))
                .filter(Objects::nonNull)
                .forEach(indexSelectorBuilder::addMethod);

        return indexSelectorBuilder.build();
    }

    private MethodSpec createMethod2(IndexDescription index,ClassName baseBean,  ClassName generator, ClassName rangeKeyGenerator) {

        ParameterizedTypeName rt = ParameterizedTypeName.get(ClassName.get(MappedQueryExpression.class), baseBean, generator);
        ParameterizedTypeName function = ParameterizedTypeName.get(ClassName.get(Function.class), rangeKeyGenerator , ParameterizedTypeName.get(ClassName.get(LogicalExpression.class),baseBean));
        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder(Optional.ofNullable(index.getName())
                        .orElse("primary"));
        methodBuilder.addParameter(ParameterSpec.builder(ClassName.bestGuess(index.getHashField().getTypeName()), "hash").build());
        methodBuilder.addParameter(ParameterSpec.builder(function, "generator").build())
                .addCode(CodeBlock.builder()
                        .add("ExpressionGenerator<$T> eg = new ExpressionGenerator<>();", baseBean)
                        .add("$T tmp1 = $T.fromS(hash);\n", AttributeValue.class, AttributeValue.class)
                        .add("Path<$T> path = (new Paths.Root()).select$L()\n;", baseBean, TypoUtils.upperCaseFirstLetter(index.getHashField().getName()))
                        .add("LogicalExpression<$T> key = new LogicalExpression.ComparisonToValue<>(path , LogicalExpression.ComparisonOperator.EQUAL, tmp1 );\n", baseBean)
                                .add("LogicalExpression<$T>  extra = eg.and(key, generator.apply(new $T()));\n", baseBean, rangeKeyGenerator)
                        .add("return new $T<>(new $T(), $S,  null, null, null, extra); \n", rt.rawType, rt.typeArguments.get(1), index.getName())
                        .build())
                .returns(rt);

        return methodBuilder.build();
    }
}
