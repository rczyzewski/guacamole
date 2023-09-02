package io.github.rczyzewski.guacamole.ddb.processor.generator;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.rczyzewski.guacamole.ddb.path.*;
import io.github.rczyzewski.guacamole.ddb.processor.TypoUtils;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class PathGenerator {
  private static final Set<String> primitives =
      new HashSet<>(Arrays.asList("String", "Integer", "Long", "Float", "Double"));
  private static final Set<DDBType> PRIMITIVE_DDB_TYPE =
      new HashSet<>(
          Arrays.asList(
              DDBType.STRING, DDBType.INTEGER, DDBType.FLOAT, DDBType.DOUBLE, DDBType.LONG));
  private static final String SELECT_METHOD = "select";

  private final ClassName pathsNamespace;

  @NotNull
  public TypeSpec createPaths(@NotNull ClassDescription classDescription) {
    TypeSpec.Builder queryClass =
        TypeSpec.classBuilder(pathsNamespace).addModifiers(PUBLIC, FINAL, STATIC);
    ClassName baseBean =
        ClassName.get(classDescription.getPackageName(), classDescription.getName());
    ParameterizedTypeName path = ParameterizedTypeName.get(ClassName.get(Path.class), baseBean);

    ClassName rootBeanPathClassName = pathsNamespace.nestedClass("Root");

    Function<Class, ParameterizedTypeName> typedParentPathFunction =
        c -> ParameterizedTypeName.get(ClassName.get(TypedPath.class), baseBean, TypeName.get(c));

    queryClass.addType(
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
              ClassName beanPathClass = pathsNamespace.nestedClass(it.getName() + "Path");
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
              queryClass.addType(pathClassBuilder.build());
            });
    // queryClass.addType
    return queryClass.build();
  }

  public TypeSpec.Builder createPath(
      ClassDescription mainBeanForRepo,
      ClassName className,
      ClassName majorBean,
      @NotNull ClassDescription classDescription,
      Function<Class, ParameterizedTypeName> typedParentPath,
      String parent) {

    ClassName baseBean =
        ClassName.get(classDescription.getPackageName(), classDescription.getName());
    ClassName mainBean = ClassName.get(mainBeanForRepo.getPackageName(), mainBeanForRepo.getName());
    TypeSpec.Builder queryClass =
        TypeSpec.classBuilder(className)
            .addAnnotation(AllArgsConstructor.class)
            .addAnnotation(Getter.class)
            .addAnnotation(Builder.class)
            .addModifiers(PUBLIC, FINAL, STATIC);
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

      } else if (fd.getTypeName().startsWith("java.util.List")) {
        MethodSpec method;
        String a = fd.getTypeArguments().get(0);
        // TODO: Contained types should be expressed as a compound object, not a string
        if (primitives.contains(a)) {
          ParameterizedTypeName returnType =
              ParameterizedTypeName.get(
                  ClassName.get(ListPath.class),
                  majorBean,
                  ParameterizedTypeName.get(ClassName.get(TerminalElement.class), majorBean));
          method =
              MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                  .addModifiers(PUBLIC)
                  .addCode(
                      "return $T.<$T, $T<$T>>builder()\n"
                          + ".provider($T::new)\n"
                          + ".selectedField(\"$L\")\n"
                          + ".parent($L)\n"
                          + ".build();\n",
                      ListPath.class,
                      baseBean,
                      TerminalElement.class,
                      baseBean,
                      TerminalElement.class,
                      fd.getAttribute(),
                      parent)
                  .returns(returnType)
                  .build();
        } else if (fd.getSourandingClasses().containsKey(a)) {
          ClassName beanPathClass = pathsNamespace.nestedClass(a + "Path");
          ParameterizedTypeName returnType =
              ParameterizedTypeName.get(ClassName.get(ListPath.class), majorBean, beanPathClass);
          method =
              MethodSpec.methodBuilder(SELECT_METHOD + TypoUtils.upperCaseFirstLetter(fd.getName()))
                  .addModifiers(PUBLIC)
                  .addComment(fd.getClassReference())
                  .addCode(
                      "return $T.<$T, $LPath>builder()\n"
                          + ".provider(it -> new $LPath(it))\n"
                          + ".selectedField(\"$L\")\n"
                          + ".parent($L)\n"
                          + ".build();\n",
                      ListPath.class,
                      majorBean,
                      a,
                      a,
                      fd.getAttribute(),
                      parent)
                  .returns(returnType)
                  .build();

        } else {
          throw new NotSupportedTypeException(
              "Only Lists of documents or "
                  + "primitives(Long,Integer,Double,String) are supported");
        }
        queryClass.addMethod(method);

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
                // .returns(typedParentPath.apply(StringBuffer.class))
                .returns(beanPathClass)
                .build();
        queryClass.addMethod(method);
      }
    }
    return queryClass;
  }
}
