package io.github.rczyzewski.guacamole.ddb.processor.generator;

import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.HASH;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.RANGE;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.FieldMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.mapper.SchemaUtils;
import io.github.rczyzewski.guacamole.ddb.processor.ClassUtils;
import io.github.rczyzewski.guacamole.ddb.processor.Logger;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

@AllArgsConstructor
public class LiveDescriptionGenerator {

  private static final long DEFAULT_READ_CAPACITY = 5;
  private static final long DEFAULT_WRITE_CAPACITY = 5;

  @NotNull private final Logger logger;

  @NotNull
  private static CodeBlock createKeySchema(
      @NotNull KeyType keyType, @NotNull String attributeName) {

    return CodeBlock.of(
        "$T.createKeySchemaElement($S, $T.$L)",
        SchemaUtils.class,
        attributeName,
        KeyType.class,
        keyType);
  }

  @NotNull
  private static CodeBlock createProjection(@NotNull ProjectionType projectionType) {
    // TODO: move all builders into helper in LiveDescrition, so here we have one liner
    return CodeBlock.builder()
        .indent()
        .add("$T.builder()", Projection.class)
        .add(".projectionType($T.$L)", ProjectionType.class, projectionType)
        .add(".build()")
        .unindent()
        .build();
  }

  @NotNull
  private static CodeBlock createThroughput(
      @NotNull Long writeCapacity, @NotNull Long readCapacity) {

    // TODO: move all builders into helper in LiveDescrition, so here we have one liner
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
      String dynamoDBName, String shortCode, boolean key, CodeBlock toJava, CodeBlock toDynamo) {

    return CodeBlock.builder()
        .indent()
        .add(
            "new $T<>($S\n, $L,\n$L,\n$L,\n$S)",
            FieldMappingDescription.class,
            dynamoDBName,
            key,
            CodeBlock.builder().indent().add(toJava).unindent().build(),
            CodeBlock.builder().indent().add(toDynamo).unindent().build(),
            shortCode)
        .unindent()
        .build();
  }

  @NotNull
  public CodeBlock create(@NotNull FieldDescription fieldDescription, ConsecutiveIdGenerator generator) {

    String suffix = TypoUtils.upperCaseFirstLetter(fieldDescription.getName());
    boolean isKeyValue = fieldDescription.isHashKey() || fieldDescription.isRangeKey();

    if (fieldDescription.getConverterClass() != null) {
      return createFieldMappingDescription(
          fieldDescription.getAttribute(),
          generator.get(),
          isKeyValue,
          CodeBlock.of("(bean, value) -> bean.with$L($T.valueOf(value))", suffix, fieldDescription.getConverterClass()),
          CodeBlock.of(
              "value->$T.ofNullable($T.toValue(value.get$L()))",
              Optional.class,
              fieldDescription.getConverterClass(),
              suffix));

    } else if (DDBType.NATIVE.equals(fieldDescription.getDdbType())) {
      return createFieldMappingDescription(
          fieldDescription.getAttribute(),
          generator.get(),
          isKeyValue,
          CodeBlock.of("(bean, value) -> bean.with$L(value)", suffix),
          CodeBlock.of("value->$T.ofNullable(value.get$L())", Optional.class, suffix));

    }
   else if (fieldDescription.getTypeArgument().fieldType().equals(FieldDescription.FieldType.LIST)) {

      return createFieldMappingDescription(
              fieldDescription.getAttribute(),
              generator.get(),
              isKeyValue,
              CodeBlock.of("(bean, value) -> bean.with$L($L.fromAttribute(value))", suffix,
                           fieldDescription.getTypeArgument().buildMapperClassName()),
              CodeBlock.of(
                      "value -> $T.ofNullable(value).map(d->d.get$L())\n" +
                              ".map(it->$L.toAttribute(it))",
                      Optional.class,
                      suffix, fieldDescription.getTypeArgument().buildMapperClassName()));

    } else if (Arrays.asList(
            DDBType.INTEGER, DDBType.DOUBLE, DDBType.FLOAT, DDBType.STRING, DDBType.LONG)
        .contains(fieldDescription.getDdbType())) {

      return createFieldMappingDescription(
          fieldDescription.getAttribute(),
          generator.get(),
          isKeyValue,
          CodeBlock.of(
              "(bean, value) ->Optional.ofNullable(value.$L()).map(it->"
                  + " bean.with$L($T.valueOf(it))).orElse(bean)",
              fieldDescription.getDdbType().getSymbol(),
              suffix,
              fieldDescription.getDdbType().getClazz()),
          CodeBlock.of(
              "value -> $T.ofNullable(value.get$L()).map(it->"
                  + " $T.builder().$L(it.toString()).build())",
              Optional.class,
              suffix,
              AttributeValue.class,
              fieldDescription.getDdbType().getSymbol()));

    } else if (null != fieldDescription.getTypeArgument().getTypeName()) {

      String liveMappingName = TypoUtils.toSnakeCase(fieldDescription.getTypeArgument().getTypeName());

      return createFieldMappingDescription(
          fieldDescription.getAttribute(),
          generator.get(),
          isKeyValue,
          CodeBlock.of(
              "(bean, value) -> bean.with$L($L.transform(value.m()))", suffix, liveMappingName),
          CodeBlock.of(
              "value -> $T.ofNullable(value.get$L()).map(it->"
                  + " $T.builder().m($L.export(it)).build())",
              Optional.class,
              suffix,
              AttributeValue.class,
              liveMappingName));

    } else {

      throw new NotSupportedTypeException(fieldDescription);
    }
  }

  TypeName get(FieldDescription.TypeArgument argument) {
    String fullTypeName = argument.getPackageName() + "." + argument.getTypeName();
    if ( argument.fieldType().equals(FieldDescription.FieldType.LIST)) {
      return ParameterizedTypeName.get(
          ClassName.get(List.class), get(argument.getTypeArguments().get(0)));
    } else if ("java.util.Map".equals(fullTypeName)) {
      return null;
    } else {
      return ClassName.get(argument.getPackageName(),argument.getTypeName()) ;
    }
  }

  @SneakyThrows
  public TypeSpec prepareMapperClass(ClassDescription classDescription, ClassName repositoryClass) {

    if( Optional.of(classDescription)
            .map(ClassDescription::getParametrized)
            .map(FieldDescription.TypeArgument::fieldType)
            .filter(FieldDescription.FieldType.LIST::equals).isPresent()){

      FieldDescription.TypeArgument dddd = classDescription.getParametrized()
              .getTypeArguments()
              .get(0);

      Optional<ClassDescription> aa = classDescription.getSourandingClasses().values().stream()
              .filter(it -> Objects.equals(it.getPackageName(), dddd.getPackageName()))
              .filter(it -> Objects.equals(it.getName(), dddd.getTypeName()))
              .findAny();

      Class<?> standardConverter =
          classDescription
              .getParametrized()
              .getTypeArguments()
              .get(0)
              .getDdbType()
              .getStandardConverterClass();

      ClassName internalMapper =
                      Optional.ofNullable(standardConverter)
                          .map(ClassName::get)
                          .orElseGet(
                                  ()->  repositoryClass.nestedClass(
                                      classDescription
                                          .getParametrized()
                                          .getTypeArguments()
                                          .get(0)
                                          .buildMapperClassName()));

      if (standardConverter == null
              && dddd.getTypeArguments().isEmpty()
              && !dddd.fieldType().equals(FieldDescription.FieldType.LIST)) {

        String mapperInstanceName =  aa.get().getName().toUpperCase();

        return TypeSpec.classBuilder(repositoryClass.nestedClass(classDescription.getGeneratedMapperName()))
                .addModifiers(STATIC)
                .addMethod(
                        MethodSpec.methodBuilder("toAttribute")
                                .addParameter(
                                        ParameterSpec.builder(get(classDescription.getParametrized()), "arg").build())
                                .addCode(" return AttributeValue.fromL( arg.stream().map(it -> $L.export(it))" +
                                             "    .map(it-> AttributeValue.fromM(it)).collect($T.toList()));",
                                        mapperInstanceName, Collectors.class)
                                .addModifiers(STATIC)
                                .returns(AttributeValue.class)
                                .build())
                .addMethod(
                        MethodSpec.methodBuilder("fromAttribute")
                                .addModifiers(STATIC)
                                .addParameter(ParameterSpec.builder(AttributeValue.class, "arg").build())
                                .addCode(
        "return  arg.l().stream().map(it->it.m()).map(it-> $L.transform(it)).collect($T.toList()); ",
                                        mapperInstanceName, Collectors.class)
                                .returns(get(classDescription.getParametrized()))
                                .build())
                .build();

      }

      return TypeSpec.classBuilder(repositoryClass.nestedClass(classDescription.getGeneratedMapperName()))
          .addModifiers(STATIC)
          .addMethod(
              MethodSpec.methodBuilder("toAttribute")
                  .addParameter(
                      ParameterSpec.builder(get(classDescription.getParametrized()), "arg").build())
                  .addCode(
                      "return AttributeValue.fromL( arg.stream().map($T::toAttribute).collect($T.toList()));\n",
                      internalMapper, Collectors.class)
                  .addModifiers(STATIC)
                  .returns(AttributeValue.class)
                  .build())
          .addMethod(
              MethodSpec.methodBuilder("fromAttribute")
                  .addModifiers(STATIC)
                  .addParameter(ParameterSpec.builder(AttributeValue.class, "arg").build())
                  .addCode(
                      "return arg.l().stream().map($T::fromAttribute).collect(Collectors.toList()); ",
                      internalMapper)
                  .returns(get(classDescription.getParametrized()))
                  .build())
          .build();
    }
    ClassName mapperClass =
            ClassName.get(repositoryClass.canonicalName(), classDescription.getGeneratedMapperName() + "CLASS");

    ClassName modelClass = ClassName.get(classDescription.getPackageName(), classDescription.getGeneratedMapperName());
    ParameterizedTypeName ptype =
        ParameterizedTypeName.get(ClassName.get(LiveMappingDescription.class), modelClass);

    return TypeSpec.classBuilder(mapperClass)
        .addModifiers(STATIC)
        .superclass(ptype)
        .addMethod(MethodSpec.constructorBuilder().addCode(createMapper(classDescription)).build())
        .build();
  }

  @NotNull
  public CodeBlock createMapper(@NotNull ClassDescription description) {

    ClassName mappedClassName = ClassName.get(description.getPackageName(), description.getName());
    ConsecutiveIdGenerator idGenerator =
        ConsecutiveIdGenerator.builder().base("ABCDEFGHIJKLMNOPRSTUWXYZ").build();
    CodeBlock indentBlocks =
        CodeBlock.builder()
            .indent()
            .add(
                description.getFieldDescriptions().stream()
                    .map(it -> this.create(it, idGenerator))
                    .collect(CodeBlock.joining(",\n ")))
            .unindent()
            .build();

    return CodeBlock.of(
        "super(()->$T.builder().build(), \n$T.asList($L));",
        mappedClassName,
        Arrays.class,
        indentBlocks);
  }

  public CodeBlock createTableDefinition(@NotNull ClassUtils utils) {
    // TODO:  only invocation of static method from LiveDescription here

    CodeBlock primary = createKeySchema(HASH, utils.getPrimaryHash().getAttribute());

    Optional<CodeBlock> secondary =
        utils.getPrimaryRange().map(it -> createKeySchema(RANGE, it.getAttribute()));

    CodeBlock attributeDefinitions =
        utils.getAttributes().stream()
            .map(
                it ->
                    CodeBlock.builder()
                        .indent()
                        .add("$T.builder()\n", AttributeDefinition.class)
                        .add(".attributeName($S)\n", it.getAttribute())
                        .add(
                            ".attributeType($S)\n",
                            it.getDdbType().getSymbol().toUpperCase(Locale.US))
                        .add(".build()\n")
                        .unindent()
                        .build())
            .collect(CodeBlock.joining(",\n"));

    CodeBlock.Builder request =
        CodeBlock.builder()
            .add("return $T.builder()\n", CreateTableRequest.class)
            .add(".tableName(tableName)\n")
            .add(
                ".keySchema($L)",
                Stream.of(Optional.of(primary), secondary)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(CodeBlock.joining(",\n")))
            .add(".attributeDefinitions($L)", attributeDefinitions)
            .add(
                ".provisionedThroughput( $L )\n",
                createThroughput(DEFAULT_WRITE_CAPACITY, DEFAULT_READ_CAPACITY));

    Optional.of(
            utils.getLSIndexes().stream()
                .map(
                    it ->
                        CodeBlock.builder()
                            .indent()
                            .add("$T.builder()\n", LocalSecondaryIndex.class)
                            .add(".indexName($S)\n", it.getLocalIndex())
                            .add(
                                ".keySchema($L, $L)\n",
                                primary,
                                createKeySchema(RANGE, it.getAttribute()))
                            .add(".projection($L)\n", createProjection(ProjectionType.ALL))
                            .add(".build()\n")
                            .unindent()
                            .build())
                .collect(CodeBlock.joining(",\n")))
        .filter(it -> !it.isEmpty())
        .ifPresent(lst -> request.add(".localSecondaryIndexes($L)\n", lst));

    Optional.of(
            utils.getGSIndexHash().stream()
                .map(
                    index ->
                        CodeBlock.builder()
                            .indent()
                            .add("$T.builder()\n", GlobalSecondaryIndex.class)
                            .add(".indexName($S)\n", index)
                            .add(".projection($L)\n", createProjection(ProjectionType.ALL))
                            .add(
                                ".provisionedThroughput( $L )\n",
                                createThroughput(DEFAULT_WRITE_CAPACITY, DEFAULT_READ_CAPACITY))
                            .add(
                                ".keySchema($L)\n",
                                Stream.of(utils.getGSIndexHash(index), utils.getGSIndexRange(index))
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .map(
                                        field ->
                                            createKeySchema(
                                                computeIndexType(field, index),
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

  public KeyType computeIndexType(FieldDescription fd, String indexName) {
    return Optional.of(fd)
        .map(FieldDescription::getGlobalIndexHash)
        .filter(it -> it.contains(indexName))
        .map(it -> HASH)
        .orElse(RANGE);
  }
}
