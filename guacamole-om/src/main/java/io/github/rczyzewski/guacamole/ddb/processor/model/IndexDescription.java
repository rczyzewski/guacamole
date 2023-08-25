package io.github.rczyzewski.guacamole.ddb.processor.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class IndexDescription {
  String name;
  FieldDescription hashField;
  FieldDescription rangeField;
  List<FieldDescription> attributes;
}
