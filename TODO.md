Plan for development of the library

#### Release 1.1
* [ ] make updates based on expressions 
* [ ] make UpdateWithExpression,method part of Repository object
* [ ] introduce Enum With all String fields
* [ ] create a method in ExpressionGenerator.equls(), that takes field
* [ ] typesafe conditional update support 
* [ ] add support/potential support for other reactive libraries
#### Release 1.2
* [ ] typesafe setting caluclated value like : value1=value1+value2 
* [ ] typesafe support for adding/removing element to the list/set
#### release 1.3
* [ ] use object mapper from amazon
* [ ] support for classes in unnamed package
#### release 1.4
* [ ] custom field type support: like Datetime
* [ ] support for ddb transactions
#### Other things

* [ ] independent code generation tests, based on manually crated
  Field/Class/Index description
* [ ] attach sonarQube
* [ ] maven report generating javadocs
* [ ] maven report with dependencies
* [ ] maven report with code coverage
* [x] run tests with maven
* [x] Remove from FieldDescription attribute: TypeMirror conversionClass;