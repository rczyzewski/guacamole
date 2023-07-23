package io.github.rczyzewski.guacamole.ddb.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.path.ListPath;
import io.github.rczyzewski.guacamole.ddb.path.Path;
import io.github.rczyzewski.guacamole.ddb.path.PrimitiveElement;
import io.github.rczyzewski.guacamole.ddb.path.TerminalElement;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class PathGenerator {
    private static final Set<String> primitives = new HashSet<>(Arrays.asList("String", "Integer", "Long", "Float", "Double"));
    private static final String REPOSITORY_SUFFIX = "Repository";
    private static final String PATH_NAMESPACE = "Paths";
    private static final String SELECT_METHOD = "select";

    @NotNull
    public TypeSpec createPaths(@NotNull ClassDescription classDescription) {
        ClassName customSearchAF = ClassName.get(classDescription.getPackageName(), classDescription.getName() +
                REPOSITORY_SUFFIX, PATH_NAMESPACE);
        TypeSpec.Builder queryClass = TypeSpec.classBuilder(customSearchAF)
                .addModifiers(PUBLIC, FINAL, STATIC);
        ClassName baseBean = ClassName.get(classDescription.getPackageName(), classDescription.getName());
        ParameterizedTypeName path =
                ParameterizedTypeName.get(ClassName.get(Path.class), baseBean);

        ClassName rootBeanPathClassName = ClassName.get(classDescription.getPackageName(),
                classDescription.getName() +
                        REPOSITORY_SUFFIX,
                PATH_NAMESPACE,
                "Root");

        queryClass.addType(createRootPath(classDescription, rootBeanPathClassName, baseBean, path, classDescription));

        classDescription.getSourandingClasses()
                .forEach((txt, it) -> {
                    ClassName beanPathClass = ClassName.get(classDescription.getPackageName(),
                            classDescription.getName() +
                                    REPOSITORY_SUFFIX,
                            PATH_NAMESPACE,
                            it.getName() + "Path");
                    queryClass.addType(createPath(classDescription, beanPathClass, baseBean, path, it));

                });
        //queryClass.addType
        return queryClass.build();
    }

    @NotNull
    public TypeSpec createRootPath(ClassDescription mainBeanForRepo, ClassName className, ClassName majorBean, TypeName parentPath, @NotNull ClassDescription classDescription) {

        ClassName baseBean = ClassName.get(mainBeanForRepo.getPackageName(), mainBeanForRepo.getName());
        TypeSpec.Builder queryClass = TypeSpec.classBuilder(className)
                .addAnnotation(AllArgsConstructor.class)
                .addAnnotation(Builder.class)
                .addModifiers(PUBLIC, FINAL, STATIC);
        for (FieldDescription fd : classDescription.getFieldDescriptions()) {
            if (fd.getDdbType() == DDBType.STRING ||
                    fd.getDdbType() == DDBType.INTEGER ||
                    fd.getDdbType() == DDBType.FLOAT ||
                    fd.getDdbType() == DDBType.DOUBLE ||
                    fd.getDdbType() == DDBType.LONG
            ) {
                MethodSpec method = MethodSpec
                        .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                        .addModifiers(PUBLIC)
                        .addCode("return $T.<$T>builder()" +
                                        ".parent(null).selectedElement(\"$L\").build();"
                                , PrimitiveElement.class, baseBean, fd.getAttribute())
                        .returns(parentPath)
                        .build();
                queryClass.addMethod(method);

            } else if (fd.getTypeName().startsWith("java.util.List")) {
                MethodSpec method;
                String a = fd.getTypeArguments().get(0);
                if (primitives.contains(a)) {
                    ParameterizedTypeName returnType =
                            ParameterizedTypeName.get(ClassName.get(ListPath.class), majorBean, ParameterizedTypeName.get(ClassName.get(TerminalElement.class), majorBean));
                    method = MethodSpec
                            .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                            .addModifiers(PUBLIC)
                            .addCode("return $T.<$T, $T<$T>>builder()\n" +
                                            ".provider($T::new)\n" +
                                            ".selectedField(\"$L\")\n" +
                                            ".parent(null)\n" +
                                            ".build();\n",
                                    ListPath.class,
                                    baseBean,
                                    TerminalElement.class,
                                    baseBean,
                                    TerminalElement.class,
                                    fd.getAttribute())
                            .returns(returnType)
                            .build();
                } else if (fd.getSourandingClasses().containsKey(a)) {
                    ClassName beanPathClass = ClassName.get(mainBeanForRepo.getPackageName(),
                            mainBeanForRepo.getName() +
                                    REPOSITORY_SUFFIX, PATH_NAMESPACE,
                            a + "Path");
                    ParameterizedTypeName returnType =
                            ParameterizedTypeName.get(ClassName.get(ListPath.class), majorBean, beanPathClass);
                    method = MethodSpec
                            .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                            .addModifiers(PUBLIC)
                            .addComment(fd.getClassReference())
                            .addCode("return $T.<$T, $LPath>builder()\n" +
                                            ".provider(it -> new $LPath(it))\n" +
                                            ".selectedField(\"$L\")\n" +
                                            ".parent(null)\n" +
                                            ".build();\n",
                                    ListPath.class,
                                    majorBean,
                                    a,
                                    a,
                                    fd.getAttribute()
                            )
                            .returns(returnType)
                            .build();

                } else {
                    throw new NotSupportedTypeException("Only Lists of documents or " +
                            "primitives(Long,Integer,Double,String) are supported");
                }
                queryClass.addMethod(method);

            } else if (fd.getDdbType() == DDBType.OTHER) {
                ClassName beanPathClass = ClassName.get(mainBeanForRepo.getPackageName(),
                        mainBeanForRepo.getName() +
                                REPOSITORY_SUFFIX,
                        PATH_NAMESPACE,
                        fd.getClassReference() + "Path");
                MethodSpec method = MethodSpec
                        .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                        .addModifiers(PUBLIC)
                        .addCode("$T<$T> element = $T.<$T>builder()" +
                                        ".parent(null).selectedElement(\"$L\").build();\n",
                                PrimitiveElement.class, baseBean, PrimitiveElement.class, baseBean, fd.getAttribute())
                        .addCode("  return $T.builder().parent(element).build();", beanPathClass)
                        .returns(beanPathClass)
                        .build();
                queryClass.addMethod(method);

            }

        }
        return queryClass
                .build();

    }

    @NotNull
    public TypeSpec createPath(ClassDescription mainBeanForRepo, ClassName className, ClassName majorBean, TypeName parentPath, @NotNull ClassDescription classDescription) {
        ClassName baseBean = ClassName.get(classDescription.getPackageName(), classDescription.getName());
        ClassName mainBean  =ClassName.get(mainBeanForRepo.getPackageName(), mainBeanForRepo.getName());
        TypeSpec.Builder queryClass = TypeSpec.classBuilder(className)
                .addSuperinterface(parentPath)
                .addAnnotation(AllArgsConstructor.class)
                .addAnnotation(Getter.class)
                .addAnnotation(Builder.class)
                .addModifiers(PUBLIC, FINAL, STATIC);
        for (FieldDescription fd : classDescription.getFieldDescriptions()) {
            if (fd.getDdbType() == DDBType.STRING ||
                    fd.getDdbType() == DDBType.INTEGER ||
                    fd.getDdbType() == DDBType.FLOAT ||
                    fd.getDdbType() == DDBType.DOUBLE ||
                    fd.getDdbType() == DDBType.LONG
            ) {
                MethodSpec method = MethodSpec
                        .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                        .addModifiers(PUBLIC)
                        .addCode("return $T.<$T>builder()" +
                                        ".parent(this).selectedElement(\"$L\").build();"
                                , PrimitiveElement.class, mainBean, fd.getAttribute())
                        .returns(parentPath)
                        .build();
                queryClass.addMethod(method);

            } else if (fd.getTypeName().startsWith("java.util.List")) {
                MethodSpec method;
                String a = fd.getTypeArguments().get(0);
                if (primitives.contains(a)) {
                    ParameterizedTypeName returnType =
                            ParameterizedTypeName.get(ClassName.get(ListPath.class), majorBean, ParameterizedTypeName.get(ClassName.get(TerminalElement.class), majorBean));
                    method = MethodSpec
                            .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                            .addModifiers(PUBLIC)
                            .addCode("return $T.<$T, $T<$T>>builder()\n" +
                                            ".provider($T::new)\n" +
                                            ".selectedField(\"$L\")\n" +
                                            ".parent(this)\n" +
                                            ".build();\n",
                                    ListPath.class,
                                    baseBean,
                                    TerminalElement.class,
                                    baseBean,
                                    TerminalElement.class,
                                    fd.getAttribute())
                            .returns(returnType)
                            .build();
                } else if (fd.getSourandingClasses().containsKey(a)) {
                    ClassName beanPathClass = ClassName.get(mainBeanForRepo.getPackageName(),
                            mainBeanForRepo.getName() +
                                    REPOSITORY_SUFFIX, PATH_NAMESPACE,
                            a + "Path");
                    ParameterizedTypeName returnType =
                            ParameterizedTypeName.get(ClassName.get(ListPath.class), majorBean, beanPathClass);
                    method = MethodSpec
                            .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                            .addModifiers(PUBLIC)
                            .addComment(fd.getClassReference())
                            .addCode("return $T.<$T, $LPath>builder()\n" +
                                            ".provider(it -> new $LPath(it))\n" +
                                            ".selectedField(\"$L\")\n" +
                                            ".parent(this)\n" +
                                            ".build();\n",
                                    ListPath.class,
                                    majorBean,
                                    a,
                                    a,
                                    fd.getAttribute()
                            )
                            .returns(returnType)
                            .build();

                } else {
                    throw new NotSupportedTypeException("Only Lists of documents or " +
                            "primitives(Long,Integer,Double,String) are supported");
                }
                queryClass.addMethod(method);

            } else if (fd.getDdbType() == DDBType.OTHER) {
                ClassName beanPathClass = ClassName.get(mainBeanForRepo.getPackageName(),
                        mainBeanForRepo.getName() +
                                REPOSITORY_SUFFIX,
                        PATH_NAMESPACE,
                        fd.getClassReference() + "Path");
                MethodSpec method = MethodSpec
                        .methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                        .addModifiers(PUBLIC)
                        .addCode("$T<$T> element = $T.<$T>builder()" +
                                        ".parent(this).selectedElement(\"$L\").build();\n",
                                PrimitiveElement.class, mainBean, PrimitiveElement.class, mainBean, fd.getAttribute())
                        .addCode("  return $T.builder().parent(element).build();", beanPathClass)
                        .returns(beanPathClass)
                        .build();
                queryClass.addMethod(method);

            }

        }
        return queryClass
                .addField(FieldSpec.builder(parentPath, "parent", PRIVATE).build())
                .addMethod(MethodSpec.methodBuilder("serialize").returns(String.class)
                        .addAnnotation(Override.class)
                        .addCode("return Optional.ofNullable(parent)\n")
                        .addCode(".map(it -> parent.serialize()).orElse(null);")
                        .addModifiers(PUBLIC).build())
                .build();

    }


}
