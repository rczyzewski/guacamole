package io.github.rczyzewski.guacamole.ddb.processor.model;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBConverted;
import io.github.rczyzewski.guacamole.ddb.processor.generator.NotSupportedTypeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@ToString
@Builder
public class FieldDescription
{
    String typeName;
    String typePackage;
    String name;
    String attribute;

    TypeMirror conversionClass;

    DDBType ddbType;
    boolean isHashKey;
    boolean isRangeKey;
    @Builder.Default
    List<String> globalIndexRange = Collections.emptyList();
    @Builder.Default
    List<String> globalIndexHash = Collections.emptyList();
    String localIndex;
    List<String>  typeArguments;


    public ClassDescription getClassDescription(){

        return this.getSourandingClasses().get(this.getClassReference());
    }
    //ClassDescription classDescription;
    @ToString.Exclude
    private final Map<String,ClassDescription> sourandingClasses;

    String classReference;

    @Getter
    @AllArgsConstructor
    public enum DDBType
    {
        S("s", String.class, true) {
            public boolean match(Element e)
            {
                return "java.lang.String".equals(e.asType().toString());
            }

        },
        C("s", String.class, true) {
            public boolean match(Element e)
            {
                return Optional.ofNullable(e.getAnnotation(DynamoDBConverted.class))
                    .isPresent();
            }
        },
        N("n", Integer.class, false) {
            public boolean match(Element e)
            {
                return "java.lang.Integer".equals(e.asType().toString());
            }

        },
        D("n", Double.class, false) {
            public boolean match(Element e)
            {
                return "java.lang.Double".equals(e.asType().toString());
            }
        },
        L("n", Long.class, false) {
            public boolean match(Element e)
            {
                return "java.lang.Long".equals(e.asType().toString());
            }
        },
        OTHER("UNKNOWN", NotSupportedTypeException.class, false) {
            public boolean match(Element e)
            {
                return true;
            }

        };

        private final String symbol;
        private final Class<?> clazz;
        private final boolean listQuerable;

        public abstract boolean match(Element e);
    }

}
