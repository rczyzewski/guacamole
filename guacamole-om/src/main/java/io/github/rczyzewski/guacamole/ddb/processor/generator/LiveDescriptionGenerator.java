package io.github.rczyzewski.guacamole.ddb.processor.generator;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.ClassUtils;
import io.github.rczyzewski.guacamole.ddb.processor.Logger;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.FieldMappingDescription;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.amazon.awssdk.services.dynamodb.model.KeyType.HASH;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE;

@AllArgsConstructor
public class LiveDescriptionGenerator
{

    private static final long DEFAULT_READ_CAPACITY = 5;
    private static final long DEFAULT_WRITE_CAPACITY = 5;

    @NotNull
    private final Logger logger;

    @NotNull
    private static CodeBlock createKeySchema(@NotNull KeyType keyType, @NotNull String attributeName)
    {

        return CodeBlock.builder()
            .indent()
            .add("$T.builder()\n", KeySchemaElement.class)
            .add(".attributeName($S)\n", attributeName)
            .add(".keyType($T.$L)\n", KeyType.class, keyType)
            .add(".build()\n")
            .unindent()
            .build();
    }

    @NotNull
    private static CodeBlock createProjection(@NotNull ProjectionType projectionType)
    {

        return CodeBlock.builder()
            .indent()
            .add("$T.builder()", Projection.class)
            .add(".projectionType($T.$L)", ProjectionType.class, projectionType)
            .add(".build()")
            .unindent()
            .build();
    }

    @NotNull
    private static CodeBlock createThroughput(@NotNull Long writeCapacity, @NotNull Long readCapacity)
    {

        return CodeBlock.builder()
            .indent()
            .add("$T.builder()\n", ProvisionedThroughput.class)
            .add(".writeCapacityUnits($LL)\n", writeCapacity)
            .add(".readCapacityUnits($LL)\n", readCapacity)
            .add(".build()")
            .unindent()
            .build();
    }

    public CodeBlock createFieldMappingDescription(
        String dynamoDBName,
        String shortCode,
        boolean key,
        CodeBlock toJava,
        CodeBlock toDynamo)
    {

        return CodeBlock.builder().indent()
            .add("new $T<>($S\n, $L,\n$L,\n$L,\n$S)",
                 FieldMappingDescription.class,
                 dynamoDBName,
                 key,
                 CodeBlock.builder().indent().add(toJava).unindent().build(),
                 CodeBlock.builder().indent().add(toDynamo).unindent().build(),
                 shortCode
            )
            .unindent()
            .build();
    }

    @NotNull
    public CodeBlock create(@NotNull FieldDescription fieldDescription, ConsecutiveIdGenerator generator)
    {

        String suffix = TypoUtils.upperCaseFirstLetter(fieldDescription.getName());
        boolean isKeyValue = fieldDescription.isHashKey() || fieldDescription.isRangeKey();

        if ("java.util.List<java.lang.String>".equals(fieldDescription.getTypeName())) {

            return createFieldMappingDescription(fieldDescription.getAttribute(), generator.get(), isKeyValue,
                                                 CodeBlock.of("(bean, value) -> bean.with$L(value.ss())", suffix),
                                                 CodeBlock.of(
                                                     "value -> $T.ofNullable(value).map(d->d.get$L()).map(it->$T.builder().ss().build())",
                                                     Optional.class, suffix, AttributeValue.class));

        } else if (fieldDescription.getTypeName().startsWith("java.util.List")) {

        String liveMappingName = TypoUtils.toSnakeCase(fieldDescription.getTypeArguments().get(0));
        return createFieldMappingDescription(fieldDescription.getAttribute(), generator.get(), isKeyValue,
                CodeBlock.of("(bean, value) -> bean.with$L(value.l().stream().map($T::m).map($L::transform).collect(Collectors.toList()))", suffix ,AttributeValue.class, liveMappingName),
                CodeBlock.of("value -> $T.ofNullable(value.get$L()).map(it->$T.builder().l(it.stream().map($L::export).map(iit -> $T.builder().m(iit).build()).collect($T.toList())).build())",
                        Optional.class, suffix, AttributeValue.class, liveMappingName, AttributeValue.class, Collectors.class));

    }

         else if (Arrays.asList(DDBType.INTEGER, DDBType.DOUBLE, DDBType.STRING, DDBType.LONG).contains(fieldDescription.getDdbType())) {

            return createFieldMappingDescription(fieldDescription.getAttribute(), generator.get(), isKeyValue,
                                                 CodeBlock.of("(bean, value) -> bean.with$L($T.valueOf(value.$L()))",
                                                              suffix,
                                                              fieldDescription.getDdbType().getClazz(),
                                                              fieldDescription.getDdbType().getSymbol()),
                                                 CodeBlock.of(
                                                     "value -> $T.ofNullable(value.get$L()).map(it-> $T.builder().$L(it.toString()).build())",
                                                     Optional.class, suffix, AttributeValue.class,
                                                     fieldDescription.getDdbType().getSymbol()));

        } else if (null != fieldDescription.getClassReference()) {

            String liveMappingName = TypoUtils.toSnakeCase( fieldDescription.getClassDescription().getName());

            return createFieldMappingDescription(fieldDescription.getAttribute(), generator.get(), isKeyValue,
                                                 CodeBlock.of("(bean, value) -> bean.with$L($L.transform(value.m()))",
                                                              suffix, liveMappingName),
                                                 CodeBlock.of(
                                                     "value -> $T.ofNullable(value.get$L()).map(it-> $T.builder().m($L.export(it)).build())",
                                                     Optional.class, suffix, AttributeValue.class, liveMappingName));

        } else {

            throw new NotSupportedTypeException(fieldDescription);
        }

    }

    @NotNull
    public List<ClassDescription> getRequiredMappers(
        @NotNull ClassDescription classDescription,
        @NotNull List<ClassDescription> encountered)
    {

        if (encountered.contains(classDescription)) {
            return encountered;
        }

        List<ClassDescription> forwarded = Stream.concat(Stream.of(classDescription), encountered.stream())
            .collect(Collectors.toList());

        return Stream.concat(
            classDescription.getFieldDescriptions()
                .stream().map(FieldDescription::getClassDescription)
                .filter(Objects::nonNull)
                .map(it -> this.getRequiredMappers(it, forwarded))
                .flatMap(List::stream),
            forwarded.stream())
            .distinct()
            .collect(Collectors.toList());
    }

    @NotNull
    public CodeBlock createMapper(@NotNull ClassDescription description)
    {

        ClassName mappedClassName = ClassName.get(description.getPackageName(), description.getName());
        ConsecutiveIdGenerator idGenerator = ConsecutiveIdGenerator.builder().base("ABCDEFGHIJKLMNOPRSTUWXYZ").build();
        CodeBlock indentBlocks = CodeBlock.builder()
            .indent()
            .add(description.getFieldDescriptions()
                     .stream()
                     .map(it -> this.create(it, idGenerator))
                     .collect(CodeBlock.joining(",\n ")))
            .unindent()
            .build();

        return CodeBlock.of("super(()->$T.builder().build(), \n$T.asList($L));",
                            mappedClassName, Arrays.class, indentBlocks);
    }

    public CodeBlock createTableDefinition(@NotNull ClassUtils utils)
    {

        CodeBlock primary = createKeySchema(HASH, utils.getPrimaryHash().getAttribute());

        CodeBlock secondary = utils.getPrimaryRange()
            .map(it -> createKeySchema(RANGE, it.getAttribute()))
            .orElse(null);

        CodeBlock attributeDefinitions = utils.getAttributes()
            .stream()
            .map(it -> CodeBlock.builder()
                .indent()
                .add("$T.builder()\n", AttributeDefinition.class)
                .add(".attributeName($S)\n", it.getAttribute())
                .add(".attributeType($S)\n", it.getDdbType().getSymbol().toUpperCase(Locale.US))
                .add(".build()\n")
                .unindent()
                .build())
            .collect(CodeBlock.joining(",\n"));

        CodeBlock.Builder request = CodeBlock.builder()
            .add("return $T.builder()\n", CreateTableRequest.class)
            .add(".tableName(tableName)\n")
            .add(".keySchema($L)", Stream.of(primary, secondary)
                .filter(Objects::nonNull)
                .collect(CodeBlock.joining(",\n")))
            .add(".attributeDefinitions($L)", attributeDefinitions)
            .add(".provisionedThroughput( $L )\n", createThroughput(DEFAULT_WRITE_CAPACITY, DEFAULT_READ_CAPACITY));

        Optional.of(utils.getLSIndexes().stream()
                        .map(it -> CodeBlock.builder()
                            .indent()
                            .add("$T.builder()\n", LocalSecondaryIndex.class)
                            .add(".indexName($S)\n", it.getLocalIndex())
                            .add(".keySchema($L, $L)\n", primary, createKeySchema(RANGE, it.getAttribute()))
                            .add(".projection($L)\n", createProjection(ProjectionType.ALL))
                            .add(".build()\n")
                            .unindent()
                            .build())
                        .collect(CodeBlock.joining(",\n")))
            .filter(it -> !it.isEmpty())
            .ifPresent(lst -> request.add(".localSecondaryIndexes($L)\n", lst));

        Optional.of(utils.getGSIndexHash()
                        .stream()
                        .map(index -> CodeBlock
                            .builder()
                            .indent()
                            .add("$T.builder()\n", GlobalSecondaryIndex.class)
                            .add(".indexName($S)\n", index)
                            .add(".projection($L)\n", createProjection(ProjectionType.ALL))
                            .add(".provisionedThroughput( $L )\n",
                                 createThroughput(DEFAULT_WRITE_CAPACITY, DEFAULT_READ_CAPACITY))
                            .add(".keySchema($L)\n",
                                 Stream.of(utils.getGSIndexHash(index), utils.getGSIndexRange(index))
                                     .filter(Optional::isPresent)
                                     .map(Optional::get)
                                     .map(field -> createKeySchema(computeIndexType(field, index),
                                                                   field.getAttribute()))
                                     .collect(CodeBlock.joining(",")))
                            .add(".build()\n")
                            .unindent()
                            .build())
                        .collect(CodeBlock.joining(",\n")))
            .filter(it -> !it.isEmpty())
            .ifPresent(indexes -> request.add(".globalSecondaryIndexes($L)\n", indexes));

        return request.add(".build();").build();
    }

    public KeyType computeIndexType(FieldDescription fd, String indexName)
    {

        return Optional.of(fd).map(FieldDescription::getGlobalIndexHash)
            .filter(it -> it.contains(indexName))
            .map(it -> HASH)
            .orElse(RANGE);

    }

}
