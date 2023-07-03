package io.github.rczyzewski.guacamole.ddb.processor.model;

import io.github.rczyzewski.guacamole.ddb.processor.generator.NotSupportedTypeException;
import lombok.AllArgsConstructor;
import lombok.Getter;

//TODO: remove it, somehow
import javax.lang.model.element.Element;

@Getter
@AllArgsConstructor
public enum DDBType
{
    STRING("s", String.class, true) {
        public boolean match(Element e)
        {
            return "java.lang.String".equals(e.asType().toString());
        }

    },
    INTEGER("n", Integer.class, false) {
        public boolean match(Element e)
        {
            return "java.lang.Integer".equals(e.asType().toString());
        }

    },
    DOUBLE("n", Double.class, false) {
        public boolean match(Element e)
        {
            return "java.lang.Double".equals(e.asType().toString());
        }
    },
    LONG("n", Long.class, false) {
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
    private final boolean isListQueryable;

    public abstract boolean match(Element e);
}
