package io.github.rczyzewski.guacamole.tests;

import io.github.rczyzewski.guacamole.LocalDateTimeConverter;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBConverted;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@DynamoDBTable
@With
public class Books {

  @DynamoDBHashKey String id;
  String title;
  List<String> authors;
  AttributeValue characteristics;
  AttributeValue notes;

  @DynamoDBConverted(converter = LocalDateTimeConverter.class)
  LocalDateTime published;

  List<String> titles;
  List<List<String>> fullAuthorNames;
  List<List<Books>> booksShelf;
  List<List<List<Books>>> gridOfBooks;
  List<List<List<List<Books>>>> fourDimensions;

  List<Books> references;
  List<AttributeValue> customArguments;
}
