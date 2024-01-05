package io.github.rczyzewski.guacamole.ddb.processor.generator;

import static io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription.FieldType.*;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.path.*;
import io.github.rczyzewski.guacamole.ddb.processor.Logger;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import java.util.*;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class PathGenerator {
  private static final Set<DDBType> PRIMITIVE_DDB_TYPE =
      new HashSet<>(
          Arrays.asList(
              DDBType.STRING, DDBType.INTEGER, DDBType.FLOAT, DDBType.DOUBLE, DDBType.LONG));
  private static final String SELECT_METHOD = "select";

  private final ClassName pathsNamespace;
  private final Logger logger;
  @NotNull
  public TypeSpec createPaths(@NotNull ClassDescription classDescription) {
    TypeSpec.Builder pathNamespace =
        TypeSpec.classBuilder(pathsNamespace).addModifiers(PUBLIC, FINAL, STATIC);

    pathNamespace.addJavadoc(
        "Set of classes that are defines access to all attributes within  "
            + classDescription.getName());

    ClassName baseBean =
        ClassName.get(classDescription.getPackageName(), classDescription.getName());
    ParameterizedTypeName path = ParameterizedTypeName.get(ClassName.get(Path.class), baseBean);

    ClassName rootBeanPathClassName = pathsNamespace.nestedClass("Root");

    Function<Class, ParameterizedTypeName> typedParentPathFunction =
        c -> ParameterizedTypeName.get(ClassName.get(TypedPath.class), baseBean, TypeName.get(c));

    pathNamespace.addType(
        createPath(
                classDescription,
                rootBeanPathClassName,
                baseBean,
                classDescription,
                typedParentPathFunction,
                "null")
            .build());

    classDescription
        .getSourandingClasses()
        .forEach(
            (txt, it) -> {
              ClassName beanPathClass = pathsNamespace.nestedClass(it.getGeneratedMapperName() + "Path");
              TypeSpec.Builder pathClassBuilder =
                  createPath(
                          classDescription,
                          beanPathClass,
                          baseBean,
                          it,
                          typedParentPathFunction,
                          "this")
                      .addField(FieldSpec.builder(path, "parent", PRIVATE).build())
                      .addSuperinterface(path)
                      .addMethod(
                          MethodSpec.methodBuilder("serialize")
                              .returns(String.class)
                              .addAnnotation(Override.class)
                              .addCode("return Optional.ofNullable(parent)\n")
                              .addCode(".map(it -> parent.serialize()).orElse(null);")
                              .addModifiers(PUBLIC)
                              .build());
              pathNamespace.addType(pathClassBuilder.build());
            });
    // queryClass.addType
    return pathNamespace.build();
  }

  public TypeSpec.Builder createPath(
      ClassDescription mainBeanForRepo,
      ClassName className,
      ClassName majorBean,
      @NotNull ClassDescription classDescription,
      Function<Class, ParameterizedTypeName> typedParentPath,
      String parent) {

    ClassName mainBean = ClassName.get(mainBeanForRepo.getPackageName(), mainBeanForRepo.getName());
    TypeSpec.Builder queryClass =
        TypeSpec.classBuilder(className)
            .addAnnotation(Getter.class)
            .addModifiers(PUBLIC, FINAL, STATIC);

    queryClass.addJavadoc(String.format("beanName: %s%n" , classDescription.getName()));
    queryClass.addJavadoc(String.format("packageName: %s%n" , classDescription.getPackageName()));
    queryClass.addJavadoc(String.format("generatedMapperName: %s%n" , classDescription.getGeneratedMapperName()));
    queryClass.addJavadoc(String.format("parametrized: %s%n" , classDescription.getParametrized()));

    if (classDescription.getParametrized() != null
        && classDescription.getParametrized().fieldType().equals(LIST)
        && classDescription.getParametrized().getTypeArguments().get(0).fieldType().equals(PRIMITIVE)) {
      ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(Path.class), majorBean);

      queryClass.addAnnotation(Builder.class);
      queryClass.addMethod(
          MethodSpec.methodBuilder("at").addModifiers(PUBLIC)
              .addParameter(ParameterSpec.builder(TypeName.get(int.class), "index").build())
              .addCode("return $T.<$T>builder().parent(this).t(index).build();", ListElement.class, majorBean)
              .returns(returnType)
              .build());

      return queryClass;
    } else if (classDescription.getParametrized() != null
            && classDescription.getParametrized().fieldType().equals(LIST)
            && classDescription.getParametrized().getTypeArguments().get(0).fieldType().equals(CUSTOM)) {

      ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(Path.class), majorBean);
      queryClass.addAnnotation(Builder.class);
      queryClass.addMethod(
              MethodSpec.methodBuilder("at").addModifiers(PUBLIC)
                      .addParameter(ParameterSpec.builder(TypeName.get(int.class), "index").build())
                      .addComment("handling Custom object")
                      .addCode("return $T.<$T>builder().parent(this).t(index).build();", ListElement.class, majorBean)
                      .returns(returnType)
                      .build());

      return queryClass;
    } else if (classDescription.getParametrized() != null
            && classDescription.getParametrized().fieldType().equals(LIST)
            && classDescription.getParametrized().getTypeArguments().get(0).fieldType().equals(LIST)) {

      ClassName returnType =
          pathsNamespace.nestedClass(
              classDescription.getParametrized().getTypeArguments().get(0).buildPathClassName());
      queryClass.addAnnotation(Builder.class);
      queryClass.addMethod(
          MethodSpec.methodBuilder("at")
              .addModifiers(PUBLIC)
              .addParameter(ParameterSpec.builder(TypeName.get(int.class), "index").build())
              .addComment("RCZ: handling List")
              .addCode("$T<$T> e ", ListElement.class, majorBean)
              .addCode("  = $T.<$T>builder().parent(this).t(index).build();\n", ListElement.class, majorBean)
              .addCode("return $T.builder().parent(e).build();", returnType)
              .returns(returnType)
              .build());

      return queryClass;
    }

    queryClass.addAnnotation(Builder.class);
           queryClass .addAnnotation(AllArgsConstructor.class);

    for (FieldDescription fd : classDescription.getFieldDescriptions()) {

      if (PRIMITIVE_DDB_TYPE.contains(fd.getDdbType())
          || fd.getDdbType().equals(DDBType.NATIVE)
          || Objects.nonNull(fd.getConverterClass())) {
        MethodSpec method =
            MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                .addModifiers(PUBLIC)
                .addCode(
                    "return $T.<$T, $T>builder()" + ".parent($L).selectedElement(\"$L\").build();",
                    PrimitiveElement.class,
                    mainBean,
                    fd.getDdbType().getClazz(),
                    parent,
                    fd.getAttribute())
                .returns(typedParentPath.apply(fd.getDdbType().getClazz()))
                .build();
        queryClass.addMethod(method);

      } else if (fd.getTypeArgument().fieldType().equals(LIST)) {

        String a = fd.getTypeArgument().getTypeArguments().get(0).getTypeName();

        // TODO: Contained types should be expressed as a compound object, not a string
        HashSet<Object> supported = new HashSet<>();
        supported.add(PRIMITIVE);
        supported.add(LIST);

        if (supported.contains(fd.getTypeArgument().getTypeArguments().get(0).fieldType())) {

         ClassName returnType = className.peerClass(fd.getTypeArgument().buildPathClassName());

          MethodSpec method =
              MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                  .addModifiers(PUBLIC)
                  .addCode(" $T<$T, AttributeValue> part =\n", PrimitiveElement.class, majorBean)
                  .addCode(
                      "$T.<$T, AttributeValue>builder()\n"
                          + "    .parent($L)\n"
                          + "    .selectedElement(\"$L\")\n"
                          + "    .build();\n",
                      PrimitiveElement.class,
                      majorBean,
                      parent,
                      fd.getAttribute())
                  .addCode("return $T.builder().parent(part).build();", returnType)
                  .returns(returnType)
                  .build();

          queryClass.addMethod(method);


      } else if (fd.getSourandingClasses().containsKey(a)) {
        ClassName beanPathClass = pathsNamespace.nestedClass(a + "Path");
        ParameterizedTypeName returnType =
                ParameterizedTypeName.get(ClassName.get(ListPath.class), majorBean, beanPathClass);
        MethodSpec method =
                MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                        .addModifiers(PUBLIC)
                        .addComment(fd.getClassReference())
                        .addComment("RCZ")
                        .addCode(
                                "return $T.<$T, $LPath>builder()\n"
                                        + ".provider(it -> new $LPath(it))\n"
                                        + ".selectedField(\"$L\")\n"
                                        + ".parent($L)\n"
                                        + ".build();\n",
                                ListPath.class, majorBean, a, a, fd.getAttribute(), parent)
                        .returns(returnType)
                        .build();
        queryClass.addMethod(method);

      }


        else {
          logger.warn(
              "Only Lists of documents or "
                  + "primitives(Long,Integer,Double,String) are supported");
          }

      } else if (fd.getDdbType() == DDBType.OTHER) {
        ClassName beanPathClass = pathsNamespace.nestedClass(fd.getClassReference() + "Path");

        ClassName Abdadfa = ClassName.bestGuess(fd.getClassReference());
        ClassName mbfr = ClassName.bestGuess(mainBeanForRepo.getName());
        MethodSpec method =
            MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                .addModifiers(PUBLIC)
                .addCode(
                    "$T<$T, $T> element = $T.<$T, $T>builder()"
                        + ".parent($L).selectedElement(\"$L\").build();\n",
                    PrimitiveElement.class,
                    mbfr,
                    Abdadfa,
                    PrimitiveElement.class,
                    mbfr,
                    Abdadfa,
                    parent,
                    fd.getAttribute())
                .addCode("  return $T.builder().parent(element).build();", beanPathClass)
                .returns(beanPathClass)
                .build();
        queryClass.addMethod(method);
      }
    }
    return queryClass;
  }
}
