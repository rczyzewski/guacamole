package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class LogicalExpressionBuilderGenerator
{
    @NotNull
    public TypeSpec createLogicalExpressionBuilder(@NotNull ClassDescription classDescription)
    {

        ClassName baseBean = ClassName.get(classDescription.getPackageName(), classDescription.getName());
        ClassName customSearchAF = ClassName.get(classDescription.getPackageName(), classDescription.getName() +
                                                                                    "Repository",
                                                 "LogicalExpressionBuilder");


        ParameterizedTypeName returnExpressionType = ParameterizedTypeName.get(ClassName.get(LogicalExpression.class),
                                                       baseBean);

        ParameterizedTypeName superInterface = ParameterizedTypeName.get(ClassName.get(ExpressionGenerator.class),
                                                       baseBean);

        TypeSpec.Builder queryClass = TypeSpec.classBuilder(customSearchAF)
                                              .addModifiers(PUBLIC, FINAL, STATIC)
                                              .superclass(superInterface);

        MethodSpec fieldExists = MethodSpec.methodBuilder("attributeExists")
                .addParameter(ParameterSpec.builder(ClassName.bestGuess("AllFields"), "value")
                        .build())
                .addModifiers(PUBLIC)
                .addCode("return new LogicalExpression.AttributeExists<>(true, value.getDdbField()); \n")
                .returns(returnExpressionType)
                .build();
        queryClass.addMethod(fieldExists);
        MethodSpec fieldDoesntExists = MethodSpec.methodBuilder("attributeNotExists")
                .addParameter(ParameterSpec.builder(ClassName.bestGuess("AllFields"), "value")
                        .build())
                .addModifiers(PUBLIC)
                .addCode("return new LogicalExpression.AttributeExists<>(false, value.getDdbField()); \n")
                .returns(returnExpressionType)
                .build();
        queryClass.addMethod(fieldDoesntExists);

       /*  MethodSpec isAttributeType = MethodSpec.methodBuilder("isAttributeType")
                .addParameter(ParameterSpec.builder(ClassName.bestGuess("AllFields"), "value")
                        .build())
                .addParameter(ParameterSpec.builder(ClassName.get(ExpressionGenerator.AttributeType.class), "type")
                        .build())
                .addModifiers(PUBLIC)
                .addCode("return null;")
                //TODO: .addCode(" return new LogicalExpression.AttributeType<>(value.getDdbField(), type); \n")
                .returns(returnExpressionType)
                .build();
        queryClass.addMethod(isAttributeType);
      */

        for (FieldDescription fd : classDescription.getFieldDescriptions()) {
            queryClass .addMethod(
                     MethodSpec
                            .methodBuilder(fd.getName() + "Exists")
                            .addModifiers(PUBLIC)
                             .addCode("return new LogicalExpression.AttributeExists<>(true, $S); \n", fd.getAttribute())
                            .returns(returnExpressionType)
                            .build());

            queryClass .addMethod(
                    MethodSpec
                            .methodBuilder(fd.getName() + "NotExists")
                            .addModifiers(PUBLIC)
                            .addCode("return new LogicalExpression.AttributeExists<>(false, $S); \n", fd.getAttribute())
                            .returns(returnExpressionType)
                            .build());

            queryClass.addMethod(MethodSpec.methodBuilder(fd.getName() + "IsAttributeType")
                    .addParameter(ParameterSpec.builder(ClassName.get(ExpressionGenerator.AttributeType.class), "type").build())
                    .addModifiers(PUBLIC)
                    .addCode("Path<$T> path = (new Paths.Root()).select$L();", baseBean, TypoUtils.upperCaseFirstLetter(fd.getName()))
                    .addCode("return new LogicalExpression.AttributeType<>(path, type); \n", fd.getAttribute())
                    .returns(returnExpressionType)
                    .build());

            if(fd.getDdbType() == DDBType.STRING){
                Arrays.stream(LogicalExpression.ComparisonOperator.values())
                      .map(it -> MethodSpec
                              .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(it.name())))
                              .addParameter(String.class, "value")
                              .addModifiers(PUBLIC)
                              .addCode("AttributeValue av =  AttributeValue.fromS(value);\n")
                              .addCode("return new LogicalExpression.ComparisonToValue<>($S,  $T.$L, av);\n",
                                       fd.getAttribute(), LogicalExpression.ComparisonOperator.class, it)
                              .returns(returnExpressionType)
                              .build()
                          )
                      .forEach(queryClass::addMethod);

                Arrays.stream(LogicalExpression.ComparisonOperator.values())
                      .map(it -> MethodSpec
                              .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(it.name())))
                              .addParameter(ParameterSpec.builder(ClassName.bestGuess("AllStrings"), "value")
                                                         .build())
                              .addModifiers(PUBLIC)
                              .addCode("return new LogicalExpression.ComparisonToReference<>($S,  $T.$L, value.getDdbField());\n",
                                       fd.getAttribute(), LogicalExpression.ComparisonOperator.class, it)
                              .returns(returnExpressionType)
                              .build()
                          )
                      .forEach(queryClass::addMethod);
            } else if (fd.getDdbType() == DDBType.INTEGER) {

                Arrays.stream(LogicalExpression.ComparisonOperator.values())
                      .map(it -> MethodSpec
                              .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(it.name())))
                              .addParameter(Integer.class, "value")
                              .addModifiers(PUBLIC)
                              .addCode("AttributeValue av =  AttributeValue.fromN(Integer.toString(value));\n")
                              .addCode("return new LogicalExpression.ComparisonToValue<>($S,  $T.$L, av);\n",
                                       fd.getAttribute(), LogicalExpression.ComparisonOperator.class, it)
                              .returns(returnExpressionType)
                              .build()
                          )
                      .forEach(queryClass::addMethod);

                Arrays.stream(LogicalExpression.ComparisonOperator.values())
                      .map(it -> MethodSpec
                              .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(it.name())))
                              .addParameter(ParameterSpec.builder(ClassName.bestGuess("FieldAllNumbers"), "value")
                                                         .build())
                              .addModifiers(PUBLIC)
                              .addCode("return new LogicalExpression.ComparisonToReference<>($S,  $T.$L, value.getDdbField());\n",
                                       fd.getAttribute(), LogicalExpression.ComparisonOperator.class, it)
                              .returns(returnExpressionType)
                              .build()
                          )
                      .forEach(queryClass::addMethod);


            }

        }

        return queryClass.build();
    }
}
