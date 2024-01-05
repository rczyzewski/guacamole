package io.github.rczyzewski.guacamole.ddb.processor.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.squareup.javapoet.ClassName;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Builder
@Value
public class FieldDescription {
  String typeName;
  String typePackage;
  //java field name
  String name;
  //ddb field name
  String attribute;

  DDBType ddbType;
  ClassName converterClass;
  boolean isHashKey;
  boolean isRangeKey;
  @Builder.Default List<String> globalIndexRange = Collections.emptyList();
  @Builder.Default List<String> globalIndexHash = Collections.emptyList();
  String localIndex;

  TypeArgument typeArgument;
  @Deprecated
  public ClassDescription getClassDescription() {
    return this.getSourandingClasses().get(this.getClassReference());
  }

  @ToString.Exclude Map<String, ClassDescription> sourandingClasses;

  String classReference;

  @Builder
  @Value
  public static class TypeArgument{
    String typeName;
    String packageName;
    String mapperUniqueId;

    DDBType ddbType;

    @Builder.Default
    List<TypeArgument>  typeArguments = Collections.emptyList();
    private String nonUniqueMapperClassName(){
      String typeArgumentNames =
              typeArguments.stream()
                      .map(TypeArgument::nonUniqueMapperClassName)
                      .collect(Collectors.joining());
      return   typeName  + typeArgumentNames;
    }
    public String buildMapperClassName(){
      return   nonUniqueMapperClassName() + mapperUniqueId;
    }
    public String buildPathClassName(){
      return   nonUniqueMapperClassName() + mapperUniqueId  + "Path";
    }
    public FieldType fieldType(){
      if(ddbType!= DDBType.OTHER) return FieldType.PRIMITIVE;
      if( "List".equals(getTypeName()) &&  "java.util".equals(getPackageName()))  return FieldType.LIST;
      return FieldType.CUSTOM;
    }
  }
  public enum FieldType{
    LIST,
    MAP,
    SET,
    CUSTOM,
    PRIMITIVE
  }
}
