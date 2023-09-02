package io.github.rczyzewski.guacamole.ddb.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.github.rczyzewski.guacamole.ddb.datamodeling.*;
import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.Types;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TableClassVisitor extends SimpleElementVisitor8<Object, Map<String, ClassDescription>> {

  private Types types;
  private Logger logger;

  ClassDescription getClassDescription(Element element) {

    HashMap<String, ClassDescription> worldContext = new HashMap<>();
    element.accept(this, worldContext);
    return worldContext.get(element.getSimpleName().toString());
  }


  public ClassName getConverterClass(Element element) {
    try {
              return Optional.of(element)
                      .map(it-> it.getAnnotation(DynamoDBConverted.class))
                              .map(DynamoDBConverted::converter)
                      .map(ClassName::get)
                      .orElse(null);

    } catch(MirroredTypeException mte) {
      DeclaredType declaredType = (DeclaredType) mte.getTypeMirror();
      TypeElement typeElement = (TypeElement ) declaredType.asElement();
      return ClassName.get(typeElement);
    }
  }

  @Override
  public TableClassVisitor visitType(TypeElement element, Map<String, ClassDescription> o) {

    String name = element.getSimpleName().toString();

    if (null != element.getAnnotation(DynamoDBTable.class)
        || null != element.getAnnotation(DynamoDBDocument.class)) {

      ClassDescription discoveredClass =
          ClassDescription.builder()
              .name(name)
              .packageName(element.getEnclosingElement().toString())
              .fieldDescriptions(new ArrayList<>())
              .sourandingClasses(o)
              .build();

      if (!o.containsKey(name)) {
        o.put(name, discoveredClass);

        element.getEnclosedElements().stream()
            .filter(it -> ElementKind.FIELD == it.getKind())
            .forEach(it -> it.accept(this, o));
      }
    }
    return this;
  }

 TypeName getParametrizedTypeName(Element element) {

   DeclaredType declaredType = ((DeclaredType) element.asType());

   TypeName[] typeArguments = declaredType.getTypeArguments()
           .stream()
           .map(it -> types.asElement(it))
           //.map(it -> (Type.TypeVar) it.asType())
           .map(this::getParametrizedTypeName)
           .map(it -> (TypeName) it)
           .toArray(TypeName[]::new);

   if(  typeArguments.length == 0 ){

      return  TypeName.get(declaredType);
   } else{

      TypeElement ddd = (TypeElement) declaredType.asElement();
      return ParameterizedTypeName.get(ClassName.get(ddd), typeArguments);
    }
  }

  @Override
  public Object visitVariable(VariableElement e, Map<String, ClassDescription> o) {

    ClassDescription classDescription = o.get(e.getEnclosingElement().getSimpleName().toString());

    DDBType ddbType =
        Arrays.stream(DDBType.values()).filter(it -> it.match(e)).findFirst().orElse(DDBType.OTHER);

    String name = e.getSimpleName().toString();
    types.asElement(e.asType()).accept(this, o);


    TypeName tn = getParametrizedTypeName(e);
    logger.warn("ddd"+ tn.toString());

    List<String> typeArguments = Collections.emptyList();

    if (e.asType() instanceof DeclaredType) {
      for (TypeMirror typeArgument : ((DeclaredType) e.asType()).getTypeArguments()) {
        types.asElement(typeArgument).accept(this, o);
      }

      typeArguments =
          ((DeclaredType) e.asType())
              .getTypeArguments().stream()
                  .map(types::asElement)
                  .map(it -> it.getSimpleName().toString())
                  .collect(Collectors.toList());
    }

    classDescription
        .getFieldDescriptions()
        .add(
            FieldDescription.builder()
                .name(name)
                .typeName(e.asType().toString())
                .typePackage(e.asType().toString())
                .ddbType(ddbType)
                .converterClass(getConverterClass(e))
                .typeArguments(typeArguments)
                .isHashKey(Optional.ofNullable(e.getAnnotation(DynamoDBHashKey.class)).isPresent())
                .isRangeKey(
                    Optional.ofNullable(e.getAnnotation(DynamoDBRangeKey.class)).isPresent())
                .localIndex(
                    Optional.ofNullable(e.getAnnotation(DynamoDBLocalIndexRangeKey.class))
                        .map(DynamoDBLocalIndexRangeKey::localSecondaryIndexName)
                        .orElse(null))
                .globalIndexHash(
                    Optional.ofNullable(e.getAnnotation(DynamoDBIndexHashKey.class))
                        .map(DynamoDBIndexHashKey::globalSecondaryIndexNames)
                        .map(Arrays::asList)
                        .orElseGet(Collections::emptyList))
                .globalIndexRange(
                    Optional.ofNullable(e.getAnnotation(DynamoDBIndexRangeKey.class))
                        .map(DynamoDBIndexRangeKey::globalSecondaryIndexNames)
                        .map(Arrays::asList)
                        .orElseGet(Collections::emptyList))
                .sourandingClasses(o)
                .classReference(((DeclaredType) e.asType()).asElement().getSimpleName().toString())
                .attribute(
                    Optional.of(DynamoDBAttribute.class)
                        .map(e::getAnnotation)
                        .map(DynamoDBAttribute::attributeName)
                        .orElse(name))
                .build());

    return this;
  }
}
