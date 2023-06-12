Plan for development of the library

#### Release 1.1
* [x] make updates based on expressions 
* [x] make UpdateWithExpression,method part of Repository object
* [x] run tests with maven
* [x] Remove from FieldDescription attribute: TypeMirror conversionClass;
* [x] introduce Enum With all String fields - missing name converrsion
* [x] create a method in ExpressionGenerator.eqauls(), that takes field
* [x] generate all conditions for Integers
* [x] generate all conditions for Strings
* [o] typesafe conditional update support 
  * [ ] implement available condition functions 
    *  [ ] size
      *   [ ] track how to prevent from creating empty lists
    *  [ ] attribute exists
    *  [ ] attribute do not exists
    *  [ ] attribute type
    *  [ ] starts with 
* [o] deploy into maven central 
* [o] provide example in a README.md 
  * [ ] table definition
  * [ ] creation of the table
  * [ ] putting data into table
  * [ ] scanning/query of the data
  * [ ] support for ddb transactions
  * [ ] updating table
  * [ ] working example within examples package 
   
### Backlog 
* [ ] validate if annotation processing works in the same way with JDK listed in SDKman. 
* [ ] find the minimal version of AWS sdk required
* [ ] typesafe setting calculated value like : value1=value1+value2 
* [ ] remove a deprecated methods
* [ ] migrating filter/keyFilter into expression style
* [ ] add support/potential support for other reactive libraries
* [ ] typesafe support for adding/removing element to the list/set
* [ ] support for classes in unnamed package
* [ ] custom field type support: like Datetime
* [ ] independent code generation tests, based on manually crated Field/Class/Index description
* [ ] attach sonarQube - attached to the local 
* [ ] maven report generating javadocs
* [ ] maven report with dependencies
* [ ] maven report with code coverage