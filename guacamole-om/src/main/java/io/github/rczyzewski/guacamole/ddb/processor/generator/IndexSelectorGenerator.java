package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.*;
import io.github.rczyzewski.guacamole.ddb.MappedQueryExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
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

            //Adding scanning filter generator
            /* ClassName scanFilterGeneratorName = externalClassName.nestedClass(Optional.ofNullable(index.getName()).orElse("_Primary"));
            TypeSpec.Builder scanFilterGenerator = TypeSpec.classBuilder(scanFilterGeneratorName);
            TypeSpec scanFilter = scanFilterGenerator.build();
            indexSelectorBuilder.addType(scanFilter);
             */

            indexSelectorBuilder.addMethod(this.createMethod(index,baseBean, logicalExpressionBuilder));

            if (index.getRangeField() != null) {
                ClassName helperSecondaryIndex = externalClassName.nestedClass(index.getRangeField().getName());
                indexSelectorBuilder.addType(createSecondryIndexSelectorHelper(helperSecondaryIndex, baseBean, index));
                indexSelectorBuilder.addMethod(createMethod2(index, helperSecondaryIndex));
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
