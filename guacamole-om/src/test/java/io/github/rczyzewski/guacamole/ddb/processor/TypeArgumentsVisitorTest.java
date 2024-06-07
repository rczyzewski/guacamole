package io.github.rczyzewski.guacamole.ddb.processor;

import io.github.rczyzewski.guacamole.ddb.mapper.ConsecutiveIdGenerator;
import io.github.rczyzewski.guacamole.ddb.processor.generator.NotSupportedTypeException;
import io.github.rczyzewski.guacamole.ddb.processor.model.DDBType;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.processor.NormalLogger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TypeArgumentsVisitorTest {

  TypeArgumentsVisitor t =
          TypeArgumentsVisitor.builder()
                  .logger(new NormalLogger())
                  .idGenerator(ConsecutiveIdGenerator.builder().build())
                  .build();


  @Test
  void visitDeclared() {

    VariableElement variableElement = Mockito.mock(VariableElement.class);
    Element element = Mockito.mock(Element.class);
    TypeMirror typeMirror = Mockito.mock(TypeMirror.class);
     when(element.asType()) .thenReturn(typeMirror);

    Name  name = Mockito.mock(Name.class);
    when(name.toString()).thenReturn("MyClassName");

    Name  packageName = Mockito.mock(Name.class);
    when(packageName.toString()).thenReturn("myPackage");

    Element enclosingElement = Mockito.mock(Element.class);

    when(element.getEnclosingElement()) .thenReturn(enclosingElement);
    when(enclosingElement.getSimpleName()).thenReturn(packageName);
    when(enclosingElement.toString()).thenReturn("named.package");

    DeclaredType declaredType = Mockito.mock(DeclaredType.class);
    when(declaredType.asElement()).thenReturn(element);
    when(element.getSimpleName()).thenReturn(name);

    FieldDescription.TypeArgument typeArgument = t.visitDeclared(declaredType, variableElement);
    assertThat(typeArgument).isEqualTo(FieldDescription.TypeArgument.builder()
                                               .typeName("MyClassName")
                                               .ddbType(DDBType.OTHER)
                                               .packageName("named.package")
                                               .mapperUniqueId("A")
                                               .build());
  }
  @Test
  void visitDeclaredWithinUnnamedPackage() {

    VariableElement variableElement = Mockito.mock(VariableElement.class);
    Element element = Mockito.mock(Element.class);
    TypeMirror typeMirror = Mockito.mock(TypeMirror.class);
    when(element.asType()) .thenReturn(typeMirror);

    Name  name = Mockito.mock(Name.class);
    when(name.toString()).thenReturn("MyClassName");

    Name  packageName = Mockito.mock(Name.class);
    when(packageName.toString()).thenReturn("myPackage");

    Element enclosingElement = Mockito.mock(Element.class);

    when(element.getEnclosingElement()) .thenReturn(enclosingElement);
    when(enclosingElement.toString()).thenReturn("unnamed package");
    when(enclosingElement.getSimpleName()).thenReturn(packageName);

    DeclaredType declaredType = Mockito.mock(DeclaredType.class);
    when(declaredType.asElement()).thenReturn(element);
    when(element.getSimpleName()).thenReturn(name);

    FieldDescription.TypeArgument typeArgument = t.visitDeclared(declaredType, variableElement);
    assertThat(typeArgument).isEqualTo(FieldDescription.TypeArgument.builder()
                                               .typeName("MyClassName")
                                               .ddbType(DDBType.OTHER)
                                               .mapperUniqueId("A")
                                               .build());
  }

    @Test
    void visitPrimitive() {
    assertThrows(NotSupportedTypeException.class,  ()-> t.visitPrimitive(null, null));

      }

    @Test
    void visitNull() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitNull(null, null));
      }

    @Test
    void visitArray() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitArray(null, null));
      }

    @Test
    void visitError() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitError(null, null));
      }

    @Test
    void visitTypeVariable() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitTypeVariable(null, null));

      }

    @Test
    void visitWildcard() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitWildcard(null, null));
      }

    @Test
    void visitExecutable() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitExecutable(null, null));
      }

    @Test
    void visitNoType() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitNoType(null, null));
      }

    @Test
    void visitUnknown() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitUnknown(null, null));
      }

    @Test
    void visitUnion() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitUnion(null, null));
      }

    @Test

    void visitIntersection() {
      assertThrows(NotSupportedTypeException.class,  ()-> t.visitIntersection(null, null));
      }
}