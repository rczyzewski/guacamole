package io.github.rczyzewski.guacamole.ddb.processor.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.squareup.javapoet.ClassName;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class FieldDescription {
  String typeName;
  String typePackage;
  String name;
  String attribute;

  DDBType ddbType;
  ClassName converterClass;
  boolean isHashKey;
  boolean isRangeKey;
  @Builder.Default List<String> globalIndexRange = Collections.emptyList();
  @Builder.Default List<String> globalIndexHash = Collections.emptyList();
  String localIndex;
  List<String> typeArguments;

  public ClassDescription getClassDescription() {

    return this.getSourandingClasses().get(this.getClassReference());
  }

  @ToString.Exclude private final Map<String, ClassDescription> sourandingClasses;

  String classReference;
}
