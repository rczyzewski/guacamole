package io.github.rczyzewski.guacamole.ddb.processor.generator;

import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;

public class NotSupportedTypeException extends RuntimeException
{
    NotSupportedTypeException(FieldDescription fieldDescription)
    {
        super(fieldDescription.toString());
    }
}
