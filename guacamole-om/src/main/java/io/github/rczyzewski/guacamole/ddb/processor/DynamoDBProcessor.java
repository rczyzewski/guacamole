package io.github.rczyzewski.guacamole.ddb.processor;

import static com.squareup.javapoet.ParameterizedTypeName.get;
import static io.github.rczyzewski.guacamole.ddb.processor.TypoUtils.toSnakeCase;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.BaseRepository;
import io.github.rczyzewski.guacamole.ddb.MappedDeleteExpression;
import io.github.rczyzewski.guacamole.ddb.MappedScanExpression;
import io.github.rczyzewski.guacamole.ddb.MappedUpdateExpression;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.processor.generator.IndexSelectorGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LiveDescriptionGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LogicalExpressionBuilderGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.PathGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/***
 * This is the entry point for generating repository, based on model classes
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DynamoDBProcessor extends AbstractProcessor {

  private Filer filer;
  private Logger logger;
  private LiveDescriptionGenerator descriptionGenerator;
  private Types types;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    super.init(processingEnvironment);
    filer = processingEnvironment.getFiler();
    logger = new CompileTimeLogger(processingEnvironment.getMessager());
    descriptionGenerator = new LiveDescriptionGenerator(logger);
    types = processingEnvironment.getTypeUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    ConsecutiveIdGenerator consecutiveIdGenerator = ConsecutiveIdGenerator.builder().build();
    TableClassVisitor classAnalyzer = new TableClassVisitor(types, logger, consecutiveIdGenerator);

    roundEnv.getElementsAnnotatedWith(DynamoDBTable.class).stream()
        .filter(it -> ElementKind.CLASS == it.getKind())
        .forEach(
            it -> {
              try {
                generateRepositoryCode(classAnalyzer.getClassDescription(it)).writeTo(filer);

              } catch (Exception e) {

                String stackTrace =
                    Arrays.stream(e.getStackTrace())
                        .map(
                            line ->
                                line.getClassName()
                                    + ":"
                                    + line.getMethodName()
                                    + ":"
                                    + line.getLineNumber())
                        .collect(Collectors.joining("\n"));

                logger.error(stackTrace);
                logger.error(
                    String.format(
                        "'%s' while processing the class: '%s'",
                        e.getMessage(), it.getSimpleName()));
              }
            });

    return true;
  }

  public JavaFile generateRepositoryCode(ClassDescription classDescription) {
    ClassName clazz =
        ClassName.get(
            Optional.ofNullable(classDescription.getPackageName()).orElse(""),
            classDescription.getName());

    ClassName repositoryClazz = clazz.peerClass(classDescription.getName() + "Repository");

    ClassName expressionBuilder = repositoryClazz.nestedClass("LogicalExpressionBuilder");

    ClassName indexSelectorName = repositoryClazz.nestedClass("IndexSelector");
    ClassName pathClassName = repositoryClazz.nestedClass("Paths");

    String mainMapperName = toSnakeCase(classDescription.getName());

    TypeSpec.Builder navigatorClass =
        TypeSpec.classBuilder(repositoryClazz)
            .addSuperinterface(get(ClassName.get(BaseRepository.class), clazz, expressionBuilder))
            .addAnnotation(Generated.class)
            .addAnnotation(Getter.class)
            .addAnnotation(Builder.class)
            .addAnnotation(AllArgsConstructor.class)
            .addModifiers(PUBLIC)
            .addField(FieldSpec.builder(get(String.class), "tableName", FINAL, PRIVATE).build())
            .addTypes(
                classDescription.getSourandingClasses().values().stream()
                    .map(it -> descriptionGenerator.prepareMapperClass(it, repositoryClazz))
                    .collect(Collectors.toList()))
            .addFields(
                classDescription.getSourandingClasses().values().stream()
                    .filter(it -> !Objects.equals(it.getPackageName(), "java.util"))
                    .map(
                        it -> {
                          ClassName newClass =
                              ClassName.get(
                                  repositoryClazz.canonicalName(),
                                  it.getGeneratedMapperName() + "CLASS");

                          return FieldSpec.builder(
                                  get(
                                      ClassName.get(LiveMappingDescription.class),
                                      ClassName.get(
                                          Optional.ofNullable(it.getPackageName()).orElse(""),
                                          it.getName())),
                                  toSnakeCase(it.getName()),
                                  STATIC,
                                  PUBLIC,
                                  FINAL)
                              .initializer(CodeBlock.builder().add("new $T()", newClass).build())
                              .build();
                        })
                    .collect(Collectors.toCollection(ArrayDeque::new)))
            .addMethod(
                MethodSpec.methodBuilder("getMapper")
                    .addModifiers(PUBLIC)
                    .addCode("return $L;", mainMapperName)
                    .returns(get(ClassName.get(LiveMappingDescription.class), clazz))
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("update")
                    .addModifiers(PUBLIC)
                    .addParameter(ParameterSpec.builder(clazz, "bean").build())
                    .addCode(
                        "return getMapper().generateUpdateExpression(bean, new $T(), tableName);",
                        expressionBuilder)
                    .returns(
                        get(ClassName.get(MappedUpdateExpression.class), clazz, expressionBuilder))
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("asWriteRequest")
                    .addModifiers(PUBLIC)
                    .addParameter(ParameterSpec.builder(ArrayTypeName.of(clazz), "arg").build())
                    .addCode("return $L.writeRequest( tableName, arg);\n", mainMapperName)
                    .varargs(true)
                    .returns(
                        get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            get(Collection.class, WriteRequest.class)))
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("create")
                    .addModifiers(PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(ParameterSpec.builder(clazz, "someName").build())
                    .addCode(
                        CodeBlock.builder()
                            .indent()
                            .add("return $T.builder()\n", PutItemRequest.class)
                            .add(".tableName(tableName)\n")
                            .add(".item($L.export(someName))\n", mainMapperName)
                            .add(".build();")
                            .unindent()
                            .build())
                    .returns(PutItemRequest.class)
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("scan")
                    .addModifiers(PUBLIC)
                    .addAnnotation(Override.class)
                    .addCode(
                        CodeBlock.builder()
                            .indent()
                            .add(
                                "return getMapper().generateScanExpression(new $T(),"
                                    + " this.tableName);",
                                expressionBuilder)
                            .unindent()
                            .build())
                    .returns(
                        get(ClassName.get(MappedScanExpression.class), clazz, expressionBuilder))
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("delete")
                    .addModifiers(PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(ParameterSpec.builder(clazz, "item").build())
                    .addCode(
                        "return getMapper().generateDeleteExpression(item, new $T(),"
                            + " this.tableName);",
                        expressionBuilder)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(MappedDeleteExpression.class), clazz, expressionBuilder))
                    .build());

    navigatorClass.addField(
        FieldSpec.builder(indexSelectorName, "indexSelector")
            .addModifiers(PRIVATE, FINAL)
            .initializer(CodeBlock.of("new $T(tableName) ", indexSelectorName))
            .addAnnotation(
                AnnotationSpec.builder(Getter.class)
                    .addMember("lazy", CodeBlock.of("true"))
                    .build())
            .build());

    ClassUtils classUtils = new ClassUtils(classDescription, logger);
    navigatorClass.addMethod(
        MethodSpec.methodBuilder("createTable")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addCode(descriptionGenerator.createTableDefinition(classUtils))
            .returns(ClassName.get(CreateTableRequest.class))
            .build());

    List<IndexDescription> indexes = classUtils.createIndexsDescription();
    TypeSpec indexSelector =
        (new IndexSelectorGenerator(classDescription))
            .createIndexSelectClass(indexSelectorName, clazz, indexes);
    navigatorClass.addType(indexSelector);

    TypeSpec queryGeneratorBuilder =
        new LogicalExpressionBuilderGenerator(classDescription)
            .createLogicalExpressionBuilder(expressionBuilder);

    PathGenerator pathGenerator = new PathGenerator(pathClassName, logger);
    @NotNull TypeSpec path = pathGenerator.createPaths(classDescription);

    navigatorClass.addType(queryGeneratorBuilder);
    navigatorClass.addType(path);

    return JavaFile.builder(
            Optional.ofNullable(classDescription.getPackageName()).orElse(""),
            navigatorClass.build())
        .addFileComment("This file is generated by `guacamole` library")
        .build();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(DynamoDBTable.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }
}
