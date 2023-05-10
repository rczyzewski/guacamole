package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.mapper.ExpressionGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LogicalExpression;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import lombok.AllArgsConstructor;
import lombok.With;
import org.jetbrains.annotations.NotNull;

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


        var returnExpressionType = ParameterizedTypeName.get(ClassName.get(LogicalExpression.class),
                                                       baseBean);

        var superInterface = ParameterizedTypeName.get(ClassName.get(ExpressionGenerator.class),
                                                       baseBean, customSearchAF);

        TypeSpec.Builder queryClass = TypeSpec.classBuilder(customSearchAF)
                                              .addModifiers(PUBLIC, FINAL, STATIC)
                                              .superclass(superInterface)
                                              .addAnnotation(With.class)

                                              .addAnnotation(AllArgsConstructor.class)
            .addInitializerBlock(CodeBlock.of("this.e = this;"));

        for (FieldDescription fd : classDescription.getFieldDescriptions()) {

            if (fd.getDdbType() == DDBType.S) {
                var m = MethodSpec.methodBuilder(fd.getName() + "Equals")
                    .addParameter(String.class, "value")
                                  .addModifiers(PUBLIC)
                                  .addCode("      return new LogicalExpression.Equal<>($S, value); \n",
                                           fd.getAttribute())
                                  .returns(returnExpressionType)
                                  .build();
                queryClass.addMethod(m);

            } else if (fd.getDdbType() == DDBType.N) {
                var m = MethodSpec.methodBuilder(fd.getName() + "LessThan")
                                  .addModifiers(PUBLIC)
                    .addParameter(Integer.class, "value")
                                  .addCode("      return new LogicalExpression.LessThanExpression<>($S, value); \n",
                                           fd.getAttribute())
                                  .returns(returnExpressionType)
                                  .build();
                queryClass.addMethod(m);

            }

        }

        return queryClass.build();
    }
}
