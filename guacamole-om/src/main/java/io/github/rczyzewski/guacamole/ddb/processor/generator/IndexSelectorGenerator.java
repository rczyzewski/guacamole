package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.MappedQueryExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression.ComparisonOperator;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@AllArgsConstructor
public class IndexSelectorGenerator {
  ClassDescription classDescription;

  public TypeSpec createIndexSelectClass(
      ClassName externalClassName, ClassName baseBean, List<IndexDescription> description) {

    TypeSpec.Builder indexSelectorBuilder = TypeSpec.classBuilder(externalClassName.simpleName());

    indexSelectorBuilder.addAnnotation(AllArgsConstructor.class);
    indexSelectorBuilder.addField(FieldSpec.builder(String.class, "table").build());

    for (IndexDescription index : description) {

      String indexName = Optional.ofNullable(index.getName()).orElse("Primary");

      ClassName helperSecondaryFilter = externalClassName.nestedClass(indexName + "Filter");
      indexSelectorBuilder.addType(
          createSecondaryIndexSelectorFilterHelper(helperSecondaryFilter, baseBean, index));

      indexSelectorBuilder.addMethod(this.createMethod(index, baseBean, helperSecondaryFilter));

      if (index.getRangeField() != null) {
        ClassName helperSecondaryIndex = externalClassName.nestedClass(indexName + "KeyFilter");
        indexSelectorBuilder.addType(
            createSecondaryIndexSelectorHelper(helperSecondaryIndex, baseBean, index));

        indexSelectorBuilder.addMethod(
            createMethod2(index, baseBean, helperSecondaryFilter, helperSecondaryIndex));
      }
    }

    return indexSelectorBuilder.build();
  }

  private MethodSpec createMethod(
      IndexDescription index, ClassName baseBean, ClassName filterClass) {
    ParameterizedTypeName rt =
        ParameterizedTypeName.get(
            ClassName.get(MappedQueryExpression.class), baseBean, filterClass);
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(Optional.ofNullable(index.getName()).orElse("primary"));
    methodBuilder
        .addParameter(
            ParameterSpec.builder(ClassName.bestGuess(index.getHashField().getTypeArgument().getTypeName()), "hash")
                .build())
        .addCode(
            CodeBlock.builder()
                .add("$T tmp1 = $T.fromS(hash);\n", AttributeValue.class, AttributeValue.class)
                .add(
                    "Path<$T> path = (new Paths.Root()).select$L()\n;",
                    baseBean,
                    TypoUtils.upperCaseFirstLetter(index.getHashField().getName()))
                .add(
                    "LogicalExpression<$T> key = new LogicalExpression.ComparisonToValue<>(path ,"
                        + " LogicalExpression.ComparisonOperator.EQUAL, tmp1 );\n",
                    baseBean)
                .add(
                    "$T ret  =  new $T<>(new $T(), $S,  tableName, null, getMapper(), key); \n",
                    rt,
                    rt.rawType,
                    rt.typeArguments.get(1),
                    index.getName())
                .build())
        .addCode("return ret;")
        .returns(rt);

    return methodBuilder.build();
  }

  private TypeSpec createSecondaryIndexSelectorFilterHelper(
      ClassName fullName, ClassName baseBean, IndexDescription index) {

    TypeSpec.Builder indexSelectorBuilder = TypeSpec.classBuilder(fullName.simpleName());
    LogicalExpressionBuilderGenerator g =
        new LogicalExpressionBuilderGenerator(this.classDescription);

    List<String> excluded =
        Stream.of(index.getHashField(), index.getRangeField())
            .filter(Objects::nonNull)
            .map(FieldDescription::getName)
            .collect(Collectors.toList());

    this.classDescription.getFieldDescriptions().stream()
        .filter(it -> !excluded.contains(it.getName()))
        .forEach(
            attribute -> {
              Arrays.stream((ComparisonOperator.values()))
                  .map(it -> g.createFilterConditionsComparedToValue(baseBean, attribute, it))
                  .filter(Objects::nonNull)
                  .forEach(indexSelectorBuilder::addMethod);

              Arrays.stream((ComparisonOperator.values()))
                  .map(it -> g.createFilterConditionsComparedToReference(baseBean, attribute, it))
                  .filter(Objects::nonNull)
                  .forEach(indexSelectorBuilder::addMethod);
            });

    return indexSelectorBuilder.build();
  }

  private TypeSpec createSecondaryIndexSelectorHelper(
      ClassName fullName, ClassName baseBean, IndexDescription index) {

    TypeSpec.Builder indexSelectorBuilder = TypeSpec.classBuilder(fullName.simpleName());
    LogicalExpressionBuilderGenerator g =
        new LogicalExpressionBuilderGenerator(this.classDescription);

    Stream.of(
            ComparisonOperator.EQUAL,
            ComparisonOperator.LESS,
            ComparisonOperator.LESS_OR_EQUAL,
            ComparisonOperator.GREATER,
            ComparisonOperator.GREATER_OR_EQUAL,
            ComparisonOperator.BETWEEN,
            ComparisonOperator.BEGINS_WITH)
        .map(it -> g.createFilterConditionsComparedToValue(baseBean, index.getRangeField(), it))
        .filter(Objects::nonNull)
        .forEach(indexSelectorBuilder::addMethod);

    return indexSelectorBuilder.build();
  }

  private MethodSpec createMethod2(
      IndexDescription index,
      ClassName baseBean,
      ClassName generator,
      ClassName rangeKeyGenerator) {

    ParameterizedTypeName rt =
        ParameterizedTypeName.get(ClassName.get(MappedQueryExpression.class), baseBean, generator);
    ParameterizedTypeName function =
        ParameterizedTypeName.get(
            ClassName.get(Function.class),
            rangeKeyGenerator,
            ParameterizedTypeName.get(ClassName.get(LogicalExpression.class), baseBean));
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(Optional.ofNullable(index.getName()).orElse("primary"));
    methodBuilder.addParameter(
        ParameterSpec.builder(ClassName.bestGuess(index.getHashField().getTypeArgument().getTypeName()), "hash")
            .build());
    methodBuilder
        .addParameter(ParameterSpec.builder(function, "generator").build())
        .addCode(
            CodeBlock.builder()
                .add("ExpressionGenerator<$T> eg = new ExpressionGenerator<>();", baseBean)
                .add("$T tmp1 = $T.fromS(hash);\n", AttributeValue.class, AttributeValue.class)
                .add(
                    "Path<$T> path = (new Paths.Root()).select$L()\n;",
                    baseBean,
                    TypoUtils.upperCaseFirstLetter(index.getHashField().getName()))
                .add(
                    "LogicalExpression<$T> key = new LogicalExpression.ComparisonToValue<>(path ,"
                        + " LogicalExpression.ComparisonOperator.EQUAL, tmp1 );\n",
                    baseBean)
                .add(
                    "LogicalExpression<$T>  extra = eg.and(key, generator.apply(new $T()));\n",
                    baseBean,
                    rangeKeyGenerator)
                .add(
                    "return new $T<>(new $T(), $S,  tableName, null, getMapper(), extra); \n",
                    rt.rawType,
                    rt.typeArguments.get(1),
                    index.getName())
                .build())
        .returns(rt);

    return methodBuilder.build();
  }
}
