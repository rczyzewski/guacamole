package io.github.rczyzewski.guacamole.ddb.processor.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class IndexDescription
{
    String name;
    FieldDescription hashField;
    FieldDescription rangeField;
    List<FieldDescription> attributes;
}
