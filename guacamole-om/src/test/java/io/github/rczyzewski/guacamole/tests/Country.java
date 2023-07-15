package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

@Value
@Builder
@DynamoDBTable
@With
class Country {
    @DynamoDBHashKey
    String id;
    String name;
    String headOfState;
    String fullName;
    Integer population;
    String famousPerson;
    String famousMusician;
    @EqualsAndHashCode.Exclude
    Float area;
    @EqualsAndHashCode.Exclude
    Double density;
}
