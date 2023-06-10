package io.github.rczyzewski.guacamole.examples.shop;

import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;

import java.util.List;

@With
@Value
@Builder
@DynamoDBTable
public class Product{

    @DynamoDBHashKey
    String uid;
    String name;
    String description;
//    @Singular("color")
    List<String> colors;
 //   @Singular("colorIE")
    List<String> colorsInEnglish;
    Integer price;
    Integer cost;
    Integer piecesAvailable;

    //TODO: Sets are not yet supported in guacamole
    //@Singular("tag")
    //Set<String> tags;

    //TODO: maps are not implemented in guacamole yet
    //@Singular("colorRef")
    //Map<Integer,String> colorsTable;
}
