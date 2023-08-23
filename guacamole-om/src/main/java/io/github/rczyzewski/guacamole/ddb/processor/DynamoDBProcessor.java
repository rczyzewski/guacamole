package io.github.rczyzewski.guacamole.ddb.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import io.github.rczyzewski.guacamole.ddb.*;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import io.github.rczyzewski.guacamole.ddb.mapper.LiveMappingDescription;
import io.github.rczyzewski.guacamole.ddb.processor.generator.IndexSelectorGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LogicalExpressionBuilderGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.PathGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;
import io.github.rczyzewski.guacamole.ddb.processor.generator.LiveDescriptionGenerator;
import lombok.*;
import lombok.experimental.StandardException;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.rczyzewski.guacamole.ddb.processor.TypoUtils.toSnakeCase;
import static com.squareup.javapoet.ParameterizedTypeName.get;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/***
 * This is the entry point for generating repository, based on model classes
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DynamoDBProcessor extends AbstractProcessor
{

    private Filer filer;
    private Logger logger;
    private LiveDescriptionGenerator descriptionGenerator;
    private PathGenerator pathGenerator;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment)
    {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        logger = new CompileTimeLogger(processingEnvironment.getMessager());
        logger.info("DDBRepoGenerator initialized");
        descriptionGenerator = new LiveDescriptionGenerator(logger);
        pathGenerator = new PathGenerator();
        types = processingEnvironment.getTypeUtils();

    }

    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv)
    {

        AnalyzerVisitor classAnalyzer = new AnalyzerVisitor(types);

        roundEnv.getElementsAnnotatedWith(DynamoDBTable.class)
                .stream()
                .filter(it -> ElementKind.CLASS == it.getKind())
                .forEach(it -> {
                    try {
                        generateRepositoryCode(classAnalyzer.generate(it)).writeTo(filer);

                    } catch (Exception e) {

                        /*
                        String stackTrace = Arrays.stream(e.getStackTrace())
                                .map(line -> line.getClassName() + ":" + line.getMethodName() + ":" + line.getLineNumber())
                                .collect(Collectors.joining("\n"));
                        */
                        logger.error(String.format( "'%s' while processing the class: '%s'" , e.getMessage()  , it.getSimpleName())) ;

                       // throw new GuacamoleRuntimeException(e);
                    }
                });

        return true;
    }

    public JavaFile generateRepositoryCode(ClassDescription classDescription)
    {
        ClassName clazz = ClassName.get(classDescription.getPackageName(), classDescription.getName());

        ClassName repositoryClazz = ClassName.get(classDescription.getPackageName(),
                                                  classDescription.getName() + "Repository");

        ClassName expressionBuilder = repositoryClazz.nestedClass("LogicalExpressionBuilder");

        ClassName indexSelectorName =  repositoryClazz.nestedClass( "IndexSelector");

        String mainMapperName = toSnakeCase(classDescription.getName());

        TypeSpec.Builder navigatorClass = TypeSpec
            .classBuilder(repositoryClazz)
            .addSuperinterface(get(ClassName.get(BaseRepository.class), clazz, expressionBuilder))
            .addAnnotation(Generated.class)
            .addAnnotation(Getter.class)
            .addAnnotation(Builder.class)
            .addAnnotation(AllArgsConstructor.class)
            .addModifiers(PUBLIC)
            .addField(FieldSpec.builder(get(String.class), "tableName", FINAL, PRIVATE).build())
                .addTypes(classDescription.getSourandingClasses().values()
                        .stream()
                        .map(it -> {
                            var  mapperClass = ClassName.get(repositoryClazz.canonicalName(), it.getName() + "CLASS");
                            var modelClass = ClassName.get(it.getPackageName(), it.getName() );
                            var ptype = get(ClassName.get( LiveMappingDescription.class ), modelClass);

                            return TypeSpec.classBuilder(mapperClass )
                                    .addModifiers(STATIC)
                                    .superclass(ptype)
                                    .addMethod(MethodSpec.constructorBuilder().addCode(
                                        descriptionGenerator.createMapper(it)).build())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .addFields(classDescription.getSourandingClasses().values()
                           .stream()
                           .map(it -> {
                               var newClass = ClassName.get(repositoryClazz.canonicalName(), it.getName() + "CLASS");

                               return FieldSpec.builder(get(ClassName.get(LiveMappingDescription.class),
                                       ClassName.get(it.getPackageName(), it.getName())),
                                       toSnakeCase(it.getName()), Modifier.STATIC, PUBLIC, FINAL)
                                       .initializer(CodeBlock.builder().add("new $T()", newClass ).build())
                                       .build();
                           })
                           .collect(Collectors.toCollection(ArrayDeque::new))
            )
            .addMethod(MethodSpec.methodBuilder("getMapper")
                        .addModifiers(PUBLIC)
                        .addCode("return $L;", mainMapperName)
                        .returns(ParameterizedTypeName.get(ClassName.get(LiveMappingDescription.class),clazz))
                        .build())
            .addMethod(MethodSpec
                    .methodBuilder("update")
                    .addModifiers(PUBLIC)
                    .addParameter(ParameterSpec.builder(clazz, "bean").build())
                    .addCode("return getMapper().generateUpdateExpression(bean, new $T(), this.tableName);", expressionBuilder )
                    .returns(ParameterizedTypeName.get(ClassName.get(MappedUpdateExpression.class), clazz,
                            expressionBuilder))
                       .build())
                .addMethod(MethodSpec.methodBuilder("asWriteRequest")
                        .addModifiers(PUBLIC)
                        .addParameter(ParameterSpec.builder(ArrayTypeName.of(clazz), "arg").build())
                                        .addCode("return $L.writeRequest( tableName, arg);\n", mainMapperName)
                        .varargs(true)
                        .returns(ParameterizedTypeName.get(ClassName.get(Map.class),ClassName.get( String.class), ParameterizedTypeName.get(Collection.class, WriteRequest.class)))
                        .build())
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(ParameterSpec.builder(clazz, "someName").build())
                        .addCode(
                                CodeBlock.builder().indent()
                                        .add("return $T.builder()\n", PutItemRequest.class)
                                        .add(".tableName(tableName)\n")
                                        .add(".item($L.export(someName))\n", mainMapperName)
                                        .add(".build();")
                                        .unindent()
                                        .build())
                        .returns(PutItemRequest.class)
                        .build())
                .addMethod(MethodSpec.methodBuilder("scan")
                        .addModifiers(PUBLIC)
                        .addAnnotation(Override.class)
                        .addCode(CodeBlock.builder()
                                .indent()
                                .add("return getMapper().generateScanExpression(new $T(), this.tableName);", expressionBuilder)
                                .unindent()
                                .build())
                        .returns(ParameterizedTypeName.get(ClassName.get(MappedScanExpression.class),clazz, expressionBuilder))
                        .build())
                .addMethod(MethodSpec.methodBuilder("delete")
                        .addModifiers(PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(ParameterSpec.builder(clazz, "item").build())
                        .addCode("return getMapper().generateDeleteExpression(item, new $T(), this.tableName);", expressionBuilder)
                        .returns(ParameterizedTypeName.get(ClassName.get(MappedDeleteExpression.class), clazz,
                                expressionBuilder))
                        .build());

        navigatorClass.addField(FieldSpec
                .builder(indexSelectorName, "indexSelector")
                .addModifiers(PRIVATE, FINAL)
                .initializer(CodeBlock.of("new $T(tableName) ", indexSelectorName)).addAnnotation(AnnotationSpec.builder(Getter.class).addMember("lazy", CodeBlock.of("true")).build()).build());

        ClassUtils classUtils = new ClassUtils(classDescription, logger);
               navigatorClass.addMethod(MethodSpec.methodBuilder("createTable")
                           .addModifiers(PUBLIC)
                           .addAnnotation(Override.class)
                           .addCode(
                               descriptionGenerator.createTableDefinition(classUtils))
                           .returns(ClassName.get(CreateTableRequest.class))
                           .build());

        List<IndexDescription> indexes = classUtils.createIndexsDescription();
        TypeSpec indexSelector = (new IndexSelectorGenerator(classDescription))
                .createIndexSelectClass(indexSelectorName, clazz, indexes);
        navigatorClass.addType(indexSelector);

       TypeSpec queryGeneratorBuilder = new LogicalExpressionBuilderGenerator(classDescription)
               .createLogicalExpressionBuilder(expressionBuilder);

        @NotNull TypeSpec path = this.pathGenerator.createPaths(classDescription);

        navigatorClass.addType(queryGeneratorBuilder);
        navigatorClass.addType(path);

        //TODO:  package name can't be empty, write test to confirm guacamole behavior
        return JavaFile.builder(classDescription.getPackageName(), navigatorClass.build())
            .addFileComment("This file is generated by `guacamole` library")
            .build();
    }



    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        return Collections.singleton(DynamoDBTable.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.RELEASE_8;
    }

    @StandardException
    static class GuacamoleRuntimeException extends RuntimeException{}
}
