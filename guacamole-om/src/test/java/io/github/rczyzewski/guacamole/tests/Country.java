package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBAttribute;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBDocument;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

@Value
@Builder
@DynamoDBTable
@With
public class Country {
  @DynamoDBHashKey String id;
  String name;

  @DynamoDBAttribute(attributeName = "PRESIDENT")
  String headOfState;

  String fullName;
  Integer population;
  String famousPerson;

  @DynamoDBAttribute(attributeName = "ROCK_STAR")
  String famousMusician;

  @EqualsAndHashCode.Exclude Float area;
  @EqualsAndHashCode.Exclude Double density;
  Capital capital;
  List<Region> regionList;

  @With
  @Value
  @Builder
  @DynamoDBDocument
  public static class Capital {
    String name;
    Long population;
  }

  @With
  @Value
  @Builder
  @DynamoDBDocument
  public static class Region {
    String name;
    Long population;
    Capital capital;
  }
}
