package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBDocument;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@DynamoDBTable
@With
public class Employee {
  @DynamoDBHashKey String id;
  String name;
  List<String> tags;
  List<Employee> employees;
  Department department;
}

@Value
@Builder
@With
@DynamoDBDocument
class Department {
  String id;
  String name;
  String location;
  Employee manager;
  List<Employee> employees;
}
