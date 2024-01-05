package io.github.rczyzewski.guacamole.ddb.processor.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ClassDescription {
  private final String name;
  private final String generatedMapperName;
  private final String packageName;
  private final List<FieldDescription> fieldDescriptions;

  private final FieldDescription.TypeArgument parametrized;

  @ToString.Exclude private final Map<String, ClassDescription> sourandingClasses;
}
