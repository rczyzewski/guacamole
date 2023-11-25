package io.github.rczyzewski.guacamole.ddb.processor;

import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.lang.model.type.*;
import javax.lang.model.util.AbstractTypeVisitor8;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
public class TypeArgumentsVisitor extends AbstractTypeVisitor8<FieldDescription.TypeArgument,Object > {

   Logger  logger;

    @Override
    public FieldDescription.TypeArgument visitPrimitive(PrimitiveType t, Object o) {
        logger.warn("Visiting primitive");
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitNull(NullType t, Object o) {
        logger.warn("Visitingnull");
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitArray(ArrayType t, Object o) {
        logger.warn("VisitArray");
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitDeclared(DeclaredType t, Object o) {

    logger.warn("Declared" + t );

    FieldDescription.TypeArgument ia =
        FieldDescription.TypeArgument.builder()
            .typeName(t.asElement().getSimpleName().toString())
            .typePackage(t.asElement().asType().toString())
            .typeArguments(
                t.getTypeArguments().stream()
                    .map(it -> (DeclaredType) it)
                    .map(it -> visitDeclared(it, o))
                    .collect(Collectors.toList()))
            .build();

    logger.warn("" + ia.toString());

    return ia;
  }

  @Override
  public FieldDescription.TypeArgument visitError(ErrorType t, Object o) {
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitTypeVariable(TypeVariable t, Object o) {
        logger.warn("Visiting type variable: " +  t + "   "+  o );
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitWildcard(WildcardType t, Object o) {
        logger.warn("visitWildcards are not supported" +  t);
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitExecutable(ExecutableType t, Object o) {
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitNoType(NoType t, Object o) {
        logger.warn("no types are not supported" +  t);
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitUnknown(TypeMirror t, Object o) {
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitUnion(UnionType t, Object o) {
        return null;
    }

    @Override
    public FieldDescription.TypeArgument visitIntersection(IntersectionType t, Object o) {
        return null;
    }
}
