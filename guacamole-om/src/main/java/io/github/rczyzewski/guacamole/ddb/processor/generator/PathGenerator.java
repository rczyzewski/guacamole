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
import io.github.rczyzewski.guacamole.ddb.path.ListElement;
import io.github.rczyzewski.guacamole.ddb.path.ListPath;
import io.github.rczyzewski.guacamole.ddb.path.Path;
import io.github.rczyzewski.guacamole.ddb.path.PrimitiveElement;
import io.github.rczyzewski.guacamole.ddb.path.TypedPath;
import io.github.rczyzewski.guacamole.ddb.processor.Logger;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        ClassName.get(Optional.ofNullable(classDescription.getPackageName()).orElse(""), classDescription.getName());

    ClassName rootBeanPathClassName = pathsNamespace.nestedClass("Root");

    pathNamespace.addType(
        createPath(rootBeanPathClassName, baseBean, classDescription, "null").build());

    classDescription
        .getSourandingClasses()
        .values()
        .forEach(
            it -> {
              ClassName beanPathClass =
                  pathsNamespace.nestedClass(it.getGeneratedMapperName() + "Path");
              ParameterizedTypeName path =
                  ParameterizedTypeName.get(ClassName.get(Path.class), baseBean);
              TypeSpec.Builder pathClassBuilder =
                  createPath(beanPathClass, baseBean, it, "this")
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
    return pathNamespace.build();
  }

  /**
   * args: className: the name of the class that will be created(ending with Path) repositoryBean:
   * the name of the class annotated with @DynamoDBTable
   */
  public TypeSpec.Builder createPath(
      ClassName className,
      ClassName repositoryBean,
      @NotNull ClassDescription classDescription,
      String parent) {

    TypeSpec.Builder queryClass =
        TypeSpec.classBuilder(className)
            .addAnnotation(Getter.class)
            .addModifiers(PUBLIC, FINAL, STATIC);

    queryClass.addJavadoc(String.format("beanName: %s%n", classDescription.getName()));
    queryClass.addJavadoc(String.format("packageName: %s%n", classDescription.getPackageName()));
    queryClass.addJavadoc(
        String.format("generatedMapperName: %s%n", classDescription.getGeneratedMapperName()));
    queryClass.addJavadoc(String.format("parametrized: %s%n", classDescription.getParametrized()));

    if (classDescription.getParametrized() != null
        && classDescription.getParametrized().fieldType().equals(LIST)
        && classDescription
            .getParametrized()
            .getTypeArguments()
            .get(0)
            .fieldType()
            .equals(PRIMITIVE)) {
      ParameterizedTypeName returnType =
          ParameterizedTypeName.get(ClassName.get(Path.class), repositoryBean);

      queryClass.addAnnotation(Builder.class);
      queryClass.addMethod(
          MethodSpec.methodBuilder("at")
              .addModifiers(PUBLIC)
              .addParameter(ParameterSpec.builder(TypeName.get(int.class), "index").build())
              .addCode(
                  "return $T.<$T>builder().parent(this).t(index).build();",
                  ListElement.class,
                  repositoryBean)
              .returns(returnType)
              .build());

      return queryClass;
    } else if (classDescription.getParametrized() != null
        && classDescription.getParametrized().fieldType().equals(LIST)
        && classDescription
            .getParametrized()
            .getTypeArguments()
            .get(0)
            .fieldType()
            .equals(CUSTOM)) {

      String a = classDescription.getParametrized().getTypeArguments().get(0).getTypeName();

      ClassName beanPathClass = pathsNamespace.nestedClass(a + "Path");

      queryClass.addAnnotation(Builder.class);
      queryClass.addMethod(
          MethodSpec.methodBuilder("at")
              .addModifiers(PUBLIC)
              .addParameter(ParameterSpec.builder(TypeName.get(int.class), "index").build())
              .addComment("handling Custom object")
              .addCode(" $T<$T> e = \n", ListElement.class, repositoryBean)
              .addCode(
                  " $T.<$T>builder() .parent(parent).t(index).build();\n",
                  ListElement.class,
                  repositoryBean)
              .addCode("return $T.builder().parent(e).build();\n", beanPathClass)
              // .addCode("//return $T.<$T>builder().parent(this).t(index).build();", beanPathClass,
              // repositoryBean)
              .returns(beanPathClass)
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
              .addCode("$T<$T> e ", ListElement.class, repositoryBean)
              .addCode(
                  "  = $T.<$T>builder().parent(this).t(index).build();\n",
                  ListElement.class,
                  repositoryBean)
              .addCode("return $T.builder().parent(e).build();", returnType)
              .returns(returnType)
              .build());

      return queryClass;
    }

    queryClass.addAnnotation(Builder.class);
    queryClass.addAnnotation(AllArgsConstructor.class);

    for (FieldDescription fd : classDescription.getFieldDescriptions()) {

      if (PRIMITIVE_DDB_TYPE.contains(fd.getDdbType())
          || fd.getDdbType().equals(DDBType.NATIVE)
          || Objects.nonNull(fd.getConverterClass())) {

        ParameterizedTypeName returnType =
            ParameterizedTypeName.get(
                ClassName.get(TypedPath.class),
                repositoryBean,
                TypeName.get(fd.getDdbType().getClazz()));

        MethodSpec method =
            MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                .addModifiers(PUBLIC)
                .addCode(
                    "return $T.<$T, $T>builder()" + ".parent($L).selectedElement(\"$L\").build();",
                    PrimitiveElement.class,
                    repositoryBean,
                    fd.getDdbType().getClazz(),
                    parent,
                    fd.getAttribute())
                .returns(returnType)
                .build();
        queryClass.addMethod(method);

      } else if (fd.getTypeArgument().fieldType().equals(LIST)) {

        String a = fd.getTypeArgument().getTypeArguments().get(0).getTypeName();

        HashSet<Object> supported = new HashSet<>();
        supported.add(PRIMITIVE);
        supported.add(LIST);

        if (supported.contains(fd.getTypeArgument().getTypeArguments().get(0).fieldType())) {

          ClassName returnType = className.peerClass(fd.getTypeArgument().buildPathClassName());

          MethodSpec method =
              MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                  .addModifiers(PUBLIC)
                  .addCode(
                      " $T<$T, AttributeValue> part =\n", PrimitiveElement.class, repositoryBean)
                  .addCode(
                      "$T.<$T, AttributeValue>builder()\n"
                          + "    .parent($L)\n"
                          + "    .selectedElement(\"$L\")\n"
                          + "    .build();\n",
                      PrimitiveElement.class,
                      repositoryBean,
                      parent,
                      fd.getAttribute())
                  .addCode("return $T.builder().parent(part).build();", returnType)
                  .returns(returnType)
                  .build();

          queryClass.addMethod(method);

        } else if (fd.getSourandingClasses().containsKey(a)) {
          ClassName beanPathClass = pathsNamespace.nestedClass(a + "Path");
          ParameterizedTypeName returnType =
              ParameterizedTypeName.get(
                  ClassName.get(ListPath.class), repositoryBean, beanPathClass);
          MethodSpec method =
              MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                  .addModifiers(PUBLIC)
                  .addCode(
                      "return $T.<$T, $LPath>builder()\n"
                          + ".provider(it -> new $LPath(it))\n"
                          + ".selectedField(\"$L\")\n"
                          + ".parent($L)\n"
                          + ".build();\n",
                      ListPath.class,
                      repositoryBean,
                      a,
                      a,
                      fd.getAttribute(),
                      parent)
                  .returns(returnType)
                  .build();
          queryClass.addMethod(method);

        } else {
          logger.warn(
              "Only Lists of documents or "
                  + "primitives(Long,Integer,Double,String) are supported");
        }

      } else if (fd.getDdbType() == DDBType.OTHER) {

        ClassName beanPathClass =
            pathsNamespace.nestedClass(fd.getTypeArgument().getTypeName() + "Path");

        MethodSpec method =
            MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                .addModifiers(PUBLIC)
                .addCode(
                    "$T<$T, $T> element = ", PrimitiveElement.class, repositoryBean, repositoryBean)
                .addCode(
                    "$T.<$T, $T>builder().parent($L).selectedElement(\"$L\").build();\n",
                    PrimitiveElement.class,
                    repositoryBean,
                    repositoryBean,
                    parent,
                    fd.getAttribute())
                .addCode("return $T.builder().parent(element).build();", beanPathClass)
                .returns(beanPathClass)
                .build();

        queryClass.addMethod(method);
      }
    }
    return queryClass;
  }
}
