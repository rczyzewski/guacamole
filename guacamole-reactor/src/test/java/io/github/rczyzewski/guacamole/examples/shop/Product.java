package io.github.rczyzewski.guacamole.examples.shop;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
@DynamoDBTable
public class Product{

    @DynamoDBHashKey
    String uid;
    String name;
    String description;
    Integer price;
    Integer cost;
    Integer piecesAvailable;
}
