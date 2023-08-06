package io.github.rczyzewski.guacamole.ddb.processor.generator;

import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.squareup.javapoet.ParameterizedTypeName.get;

public class FilterMethodsCreator
{

    public static List<MethodSpec> createKeyFiltersMethod(
        ClassName className,
        IndexDescription description)
    {

        return Stream.of(description.getHashField(), description.getRangeField())
            .filter(Objects::nonNull)
            .filter(it -> it.getDdbType() != DDBType.OTHER)
            .flatMap(field -> Stream.of(Operator.values())
                .map(op -> op.createMethod(className, field)))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static MethodSpec createNoArgMethod(ClassName className, FieldDescription fd, Operator op)
    {

        String methodName = fd.getName() + TypoUtils.upperCaseFirstLetter(op.getValue());
        return MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(className)
            .addCode(CodeBlock
                         .of("conditionHashMap.put($S,$L);\n", fd.getAttribute(),
                             CodeBlock.builder()
                                 .add("$T.builder()\n", Condition.class)
                                 .add(".comparisonOperator($T.$L)\n", ComparisonOperator.class, op.getDdbOperator())
                                 .add(".build()")
                                 .build()))
            .addCode("return this;")
            .build();
    }

    public static MethodSpec createSingleArgMethod(ClassName className, FieldDescription fd, Operator op)
    {

        String methodName = fd.getName() + TypoUtils.upperCaseFirstLetter(op.getValue());

        CodeBlock coreBlock = CodeBlock.of("$T.builder().$L(String.valueOf(property)).build()",
                                                       AttributeValue.class, fd.getDdbType().getSymbol());

        return MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassName.bestGuess(fd.getTypeName()), "property")

            .returns(className)
            .addCode("if(  null !=property ){ ", String.class)
            .addCode(CodeBlock
                         .of("conditionHashMap.put($S,$L);\n", fd.getAttribute(),
                             CodeBlock.builder()
                                 .add("$T.builder()\n", Condition.class)
                                 .add(".comparisonOperator($T.$L)\n", ComparisonOperator.class,
                                      op.getDdbOperator())
                                 .add(".attributeValueList($L)\n", coreBlock)
                                 .add(".build()")
                                 .build()))
            .addCode("}")
            .addCode("return this;")
            .build();

    }

    @AllArgsConstructor
    @Getter
    public enum Operator
    {
        EQ("equals", ComparisonOperator.EQ),
        NE("notEquals", ComparisonOperator.NE),
        LE("lessOrEqual", ComparisonOperator.LE),
        LT("lessThen", ComparisonOperator.LT),
        GE("graterOrEquals", ComparisonOperator.GE),
        GT("graterThan", ComparisonOperator.GT),
        IN("in", ComparisonOperator.IN) {
            @Override
            public MethodSpec createMethod(ClassName className, FieldDescription fd)
            {

                ParameterizedTypeName aa = get(ClassName.get(List.class), ClassName.get(fd.getDdbType().getClazz()));

                String methodName = fd.getName() + TypoUtils.upperCaseFirstLetter(this.getValue());
                return MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(aa, "property")
                    .returns(className)
                    .addCode(CodeBlock
                                 .of("conditionHashMap.put($S,$L);\n", fd.getAttribute(),
                                     CodeBlock.builder()
                                         .add("$T.builder()\n", Condition.class)
                                         .add(".comparisonOperator($T.$L)\n", ComparisonOperator.class,
                                              this.getDdbOperator())
                                         .add(
                                             ".attributeValueList(property.stream().map(it-> $T.builder().$L(String.valueOf(it)).build()).collect($T.toList()))\n",
                                             AttributeValue.class, fd.getDdbType().getSymbol(), Collectors.class)
                                         .add(".build()")
                                         .build()))
                    .addCode("return this;")
                    .build();
            }

        },
        BETWEEN("between", ComparisonOperator.BETWEEN) {
            @Override
            public MethodSpec createMethod(ClassName className, FieldDescription fd)
            {

                MethodSpec geMethod = GE.createMethod(className, fd);
                MethodSpec leMethod = LE.createMethod(className, fd);

                CodeBlock coreBlock =CodeBlock.of(
                    "$T.builder().$L(String.valueOf(begin)).build(), $T.builder().$L(String.valueOf(end)).build()",
                    AttributeValue.class,
                    fd.getDdbType().getSymbol(),
                    AttributeValue.class,
                    fd.getDdbType().getSymbol());


                String methodName = fd.getName() + TypoUtils.upperCaseFirstLetter(this.getValue());
                return MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.bestGuess(fd.getTypeName()), "begin")
                    .addParameter(ClassName.bestGuess(fd.getTypeName()), "end")
                    .returns(className)

                    .addCode("if( begin != null  && end != null ) {")
                    .addCode(CodeBlock
                                 .of("conditionHashMap.put($S,$L);\n", fd.getAttribute(),
                                     CodeBlock.builder()
                                         .add("$T.builder()\n", Condition.class)
                                         .add(".comparisonOperator($T.$L)\n", ComparisonOperator.class,
                                              this.getDdbOperator())
                                         .add(".attributeValueList($L)\n", coreBlock)
                                         .add(".build()")
                                         .build()))
                    .addCode("} else if ( begin != null ) {\n")
                    .addCode("$L(begin);\n", geMethod.name)
                    .addCode("} else if ( end != null ) {\n")
                    .addCode("$L(end);\n", leMethod.name)
                    .addCode("}\n")
                    .addCode("return this;\n")
                    .build();
            }

        },
        NOT_NULL("isNotNull", ComparisonOperator.NOT_NULL) {
            @Override
            public MethodSpec createMethod(ClassName ret, FieldDescription fieldDescription)
            {
                return createNoArgMethod(ret, fieldDescription, this);
            }
        },
        NULL("isNull", ComparisonOperator.NULL) {
            @Override
            public MethodSpec createMethod(ClassName ret, FieldDescription fieldDescription)
            {
                return createNoArgMethod(ret, fieldDescription, this);
            }

        },
        CONTAINS("contains", ComparisonOperator.CONTAINS) {
            //TODO CONTAINS, NOT_CONTAINS, BEGINS_WITH are copy pasted!!!
            @Override
            public MethodSpec createMethod(ClassName ret, FieldDescription fieldDescription)
            {
                if (fieldDescription.getDdbType().isListQueryable()) {
                    return createSingleArgMethod(ret, fieldDescription, this);
                } else {
                    return null;
                }
            }

        },
        NOT_CONTAINS("notContains", ComparisonOperator.NOT_CONTAINS) {
            @Override
            public MethodSpec createMethod(ClassName ret, FieldDescription fieldDescription)
            {
                if (fieldDescription.getDdbType().isListQueryable()) {
                    return createSingleArgMethod(ret, fieldDescription, this);
                } else {
                    return null;
                }
            }

        },
        BEGINS_WITH("beginsWith", ComparisonOperator.BEGINS_WITH) {
            @Override
            public MethodSpec createMethod(ClassName ret, FieldDescription fieldDescription)
            {
                if (fieldDescription.getDdbType().isListQueryable()) {
                    return createSingleArgMethod(ret, fieldDescription, this);
                } else {
                    return null;
                }
            }

        };

        private final String value;
        private final ComparisonOperator ddbOperator;

        public MethodSpec createMethod(ClassName ret, FieldDescription fieldDescription)
        {
            return createSingleArgMethod(ret, fieldDescription, this);
        }
    }

}
