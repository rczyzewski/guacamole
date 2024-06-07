package io.github.rczyzewski.guacamole.ddb.processor;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.NotSupportedTypeException;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor8;
import java.util.Arrays;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
public class TypeArgumentsVisitor
    extends AbstractTypeVisitor8<FieldDescription.TypeArgument, VariableElement> {

  Logger logger;
  ConsecutiveIdGenerator idGenerator;

  @Override
  public FieldDescription.TypeArgument visitDeclared(DeclaredType t, VariableElement o) {

    DDBType ddbType =
        Arrays.stream(DDBType.values())
            .filter(it -> it.match(t.asElement()))
            .findFirst()
            .orElse(DDBType.OTHER);

    String mapperUniqueId = idGenerator.get();
    // TODO:: adding getSimpleName do not work - causing Path generation to fail
    String packageName = t.asElement().getEnclosingElement().toString();
    if (packageName.contains(" ")) {
      packageName = null;
    }

    return FieldDescription.TypeArgument.builder()
        .typeName(t.asElement().getSimpleName().toString())
        .packageName(packageName)
        .mapperUniqueId(mapperUniqueId)
        .ddbType(ddbType)
        .typeArguments(
            t.getTypeArguments().stream()
                .map(it -> (DeclaredType) it)
                .map(it -> visitDeclared(it, o))
                .collect(Collectors.toList()))
        .build();
  }

  @Override
  public FieldDescription.TypeArgument visitPrimitive(PrimitiveType t, VariableElement o) {
    String msg = "'primitives' are not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitNull(NullType t, VariableElement o) {
    String msg = "'null' are not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitArray(ArrayType t, VariableElement o) {
    String msg = "'arrays' are not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitError(ErrorType t, VariableElement o) {
    String msg = "'errorType' are not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitTypeVariable(TypeVariable t, VariableElement o) {
    String msg = "typeVariable are not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitWildcard(WildcardType t, VariableElement o) {
    String msg = "wildcards are not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitExecutable(ExecutableType t, VariableElement o) {
    String msg = "executable are not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitNoType(NoType t, VariableElement o) {
    String msg = "noType is not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitUnknown(TypeMirror t, VariableElement o) {
   String msg = "unknown is not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg , o);
  }

  @Override
  public FieldDescription.TypeArgument visitUnion(UnionType t, VariableElement o) {
    String msg = "union is not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }

  @Override
  public FieldDescription.TypeArgument visitIntersection(IntersectionType t, VariableElement o) {
    String msg = "intersection is not supported";
    logger.error(msg, o);
    throw new NotSupportedTypeException(msg, o);
  }
}
