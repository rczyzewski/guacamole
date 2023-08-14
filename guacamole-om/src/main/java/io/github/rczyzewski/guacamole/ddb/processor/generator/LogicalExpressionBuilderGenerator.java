package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.*;
import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.path.Path;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class LogicalExpressionBuilderGenerator {
    @NotNull
    public List<MethodSpec> createExistsAndTypeConditions(@NotNull ClassName baseBean, @NotNull FieldDescription fd) {
        ParameterizedTypeName returnExpressionType = ParameterizedTypeName.get(ClassName.get(LogicalExpression.class),
                baseBean);

        ArrayList<MethodSpec> ret = new ArrayList<>();

        CodeBlock cb = CodeBlock.of("Path<$T> path = (new Paths.Root()).select$L()\n;"
                , baseBean, TypoUtils.upperCaseFirstLetter(fd.getName()));

        ret.add(MethodSpec
                .methodBuilder(fd.getName() + "Exists")
                .addModifiers(PUBLIC)
                .addCode(cb)
                .addCode("return new LogicalExpression.AttributeExists<>(true, path); \n", fd.getAttribute())
                .returns(returnExpressionType)
                .build());

        ret.add(MethodSpec
                .methodBuilder(fd.getName() + "NotExists")
                .addModifiers(PUBLIC)
                .addCode(cb)
                .addCode("return new LogicalExpression.AttributeExists<>(false, path); \n")
                .returns(returnExpressionType)
                .build());

        ret.add(MethodSpec.methodBuilder(fd.getName() + "IsAttributeType")
                .addParameter(ParameterSpec.builder(ClassName.get(ExpressionGenerator.AttributeType.class), "type").build())
                .addModifiers(PUBLIC)
                .addCode(cb)
                .addCode("return new LogicalExpression.AttributeType<>(path, type); \n", fd.getAttribute())
                .returns(returnExpressionType)
                .build());

        return ret;
    }

    public MethodSpec createFilterConditionsComparedToValue(@NotNull ClassName baseBean,
                                                            @NotNull FieldDescription fd,
                                                            LogicalExpression.ComparisonOperator operator) {
        ParameterizedTypeName returnExpressionType = ParameterizedTypeName.get(ClassName.get(LogicalExpression.class),
                baseBean);

        if (fd.getDdbType() != DDBType.STRING && !Objects.equals(fd.getDdbType().getSymbol(), "n"))
            return null;

        CodeBlock cb = fd.getDdbType() == DDBType.STRING ?
                CodeBlock.of("AttributeValue av =  AttributeValue.fromS(value);\n") :
                CodeBlock.of("AttributeValue av =  AttributeValue.fromN($T.toString(value));\n", fd.getDdbType().getClazz());

        CodeBlock path = CodeBlock.of("Path<$T> path = (new Paths.Root()).select$L()\n;", baseBean, TypoUtils.upperCaseFirstLetter(fd.getName()));

        if (LogicalExpression.ComparisonOperator.BEGINS_WITH.equals(operator))
        {
            if( Objects.equals(fd.getDdbType().getSymbol() ,  "n")) {
                return null;
            }

            return MethodSpec
                    .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(operator.name())))
                    .addParameter(fd.getDdbType().getClazz(), "value")
                    .addModifiers(PUBLIC)
                    .addCode(cb)
                    .addCode(path)
                    .addCode("return new LogicalExpression.BeginsWith<>(path,  av);\n")
                    .returns(returnExpressionType)
                    .build();



        } else if (LogicalExpression.ComparisonOperator.BETWEEN.equals(operator)) {
            CodeBlock cb2 = fd.getDdbType() == DDBType.STRING ?
                    CodeBlock.of("AttributeValue av2 =  AttributeValue.fromS(value2);\n") :
                    CodeBlock.of("AttributeValue av2 =  AttributeValue.fromN($T.toString(value2));\n", fd.getDdbType().getClazz());

            return MethodSpec
                    .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(operator.name())))
                    .addParameter(fd.getDdbType().getClazz(), "value")
                    .addParameter(fd.getDdbType().getClazz(), "value2")
                    .addModifiers(PUBLIC)
                    .addCode(cb)
                    .addCode(cb2)
                    .addCode(path)
                    .addCode("return new LogicalExpression.Between<>(path,   av, av2);\n" )
                    .returns(returnExpressionType)
                    .build();

        }


        return MethodSpec
                .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(operator.name())))
                .addParameter(fd.getDdbType().getClazz(), "value")
                .addModifiers(PUBLIC)
                .addCode(cb)
                .addCode(path)
                .addCode("return new LogicalExpression.ComparisonToValue<>(path,  $T.$L, av);\n",
                        LogicalExpression.ComparisonOperator.class, operator)
                .returns(returnExpressionType)
                .build();
    }

    public MethodSpec createFilterConditionsComparedToReference(@NotNull ClassName baseBean,
                                                                @NotNull FieldDescription fd,
                                                                LogicalExpression.ComparisonOperator operator) {
        ParameterizedTypeName returnExpressionType = ParameterizedTypeName.get(ClassName.get(LogicalExpression.class),
                baseBean);

        ParameterizedTypeName pathOfBean = ParameterizedTypeName.get(ClassName.get(Path.class), baseBean);

        if (fd.getDdbType() != DDBType.STRING && !Objects.equals(fd.getDdbType().getSymbol(), "n"))
            return null;

        if (fd.getDdbType() != DDBType.STRING  && operator.equals(LogicalExpression.ComparisonOperator.BEGINS_WITH)) {
            return null; //Because BeginsWith make sense only with STRING
        }
        if(operator.equals(LogicalExpression.ComparisonOperator.BETWEEN)) {
            // right now between is only supported for two values,
            // support can be extended for path and value and for two paths,
            // but I don't think it's a priority
            return null;
        }


        return MethodSpec
                .methodBuilder(fd.getName() + TypoUtils.upperCaseFirstLetter(TypoUtils.toCamelCase(operator.name())))
                .addParameter(ParameterSpec.builder(pathOfBean, "referencePath").build())
                .addModifiers(PUBLIC)
                .addCode("Path<$T> path = (new Paths.Root()).select$L()\n;", baseBean, TypoUtils.upperCaseFirstLetter(fd.getName()))
                .addCode("return new LogicalExpression.ComparisonToReference<>(path,  $T.$L, referencePath);\n",
                        LogicalExpression.ComparisonOperator.class, operator)
                .returns(returnExpressionType)
                .build();
    }

    @NotNull
    public TypeSpec createLogicalExpressionBuilder(ClassName customSearchAF, @NotNull ClassDescription classDescription) {

        ClassName baseBean = ClassName.get(classDescription.getPackageName(), classDescription.getName());

        ParameterizedTypeName superInterface = ParameterizedTypeName.get(ClassName.get(ExpressionGenerator.class),
                baseBean);

        TypeSpec.Builder queryClass = TypeSpec.classBuilder(customSearchAF)
                .addModifiers(PUBLIC, FINAL, STATIC)
                .superclass(superInterface);


        for (FieldDescription fd : classDescription.getFieldDescriptions()) {
            queryClass.addMethods(createExistsAndTypeConditions(baseBean, fd));

            Arrays.stream(LogicalExpression.ComparisonOperator.values())
                    .map(it -> this.createFilterConditionsComparedToValue(baseBean, fd, it))
                    .filter(Objects::nonNull)
                    .forEach(queryClass::addMethod);

            Arrays.stream(LogicalExpression.ComparisonOperator.values())
                    .map(it -> this.createFilterConditionsComparedToReference(baseBean, fd, it))
                    .filter(Objects::nonNull)
                    .forEach(queryClass::addMethod);

        }
        return queryClass.build();
    }
}

