package io.github.rczyzewski.guacamole.tests;

import static org.assertj.core.api.Assertions.*;

import io.github.rczyzewski.guacamole.ddb.path.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class PathTest {

  EmployeeRepository.Paths.Root employeePathCreator = new EmployeeRepository.Paths.Root();
  CountryRepository.Paths.CountryPath countryPathCreator =
      CountryRepository.Paths.CountryPath.builder().build();

  @Test
  void validatingPaths() {

    assertThat(employeePathCreator.selectEmployees().serialize()).isEqualTo("employees");

    assertThat(employeePathCreator.selectEmployees().at(2).serialize()).isEqualTo("employees[2]");

    assertThat(employeePathCreator.selectEmployees().at(2).selectEmployees().serialize())
        .isEqualTo("employees[2].employees");
    assertThat(
            employeePathCreator
                .selectDepartment()
                .selectEmployees()
                .at(1)
                .selectEmployees()
                .serialize())
        .isEqualTo("department.employees[1].employees");

    assertThat(
            employeePathCreator
                .selectDepartment()
                .selectManager()
                .selectEmployees()
                .at(0)
                .selectName()
                .serialize())
        .isEqualTo("department.manager.employees[0].name");
  }

  @Test
  void validatingPathsWithATopLevelFields() {

    assertThat(countryPathCreator.selectId().serialize()).isEqualTo("id");

    assertThat(countryPathCreator.selectName().serialize()).isEqualTo("name");

    assertThat(countryPathCreator.selectHeadOfState().serialize()).isEqualTo("PRESIDENT");

    assertThat(countryPathCreator.selectFullName().serialize()).isEqualTo("fullName");

    assertThat(countryPathCreator.selectPopulation().serialize()).isEqualTo("population");

    assertThat(countryPathCreator.selectFamousPerson().serialize()).isEqualTo("famousPerson");

    assertThat(countryPathCreator.selectFamousMusician().serialize()).isEqualTo("ROCK_STAR");

    assertThat(countryPathCreator.selectArea().serialize()).isEqualTo("area");

    assertThat(countryPathCreator.selectDensity().serialize()).isEqualTo("density");
  }

  @Test
  void ensureThatRootPathIsLike() {
    EmployeeRepository.Paths.EmployeePath rp =
        EmployeeRepository.Paths.EmployeePath.builder().build();
    assertThat(rp.selectId().serialize()).isEqualTo("id");
    assertThat(rp.selectName().serialize()).isEqualTo("name");
    assertThat(rp.selectTags().serialize()).isEqualTo("tags");
    assertThat(rp.selectEmployees().serialize()).isEqualTo("employees");
    assertThat(rp.selectDepartment().serialize()).isEqualTo("department");
  }

  @Test
  void selectStringElementFromList() {
    EmployeeRepository.Paths.EmployeePath rp =
        EmployeeRepository.Paths.EmployeePath.builder().build();
    assertThat(rp.selectTags().at(4).serialize()).isEqualTo("tags[4]");
  }

  @Test
  void selectCompoundElementFromList() {
    assertThat(employeePathCreator.selectEmployees().at(5).serialize()).isEqualTo("employees[5]");
    assertThat(employeePathCreator.selectEmployees().at(5).selectName().serialize())
        .isEqualTo("employees[5].name");
  }

  @Test
  void ensureThatPathPartsAreGeneratedCorrectly() {
    Path<Employee> path = employeePathCreator.selectEmployees().at(5).selectName();

    Set<String> parts = path.getPartsName();
    assertThat(parts).contains("employees", "name");

    Map<String, String> stringMap = new HashMap<>();
    stringMap.put("employees", "A");
    stringMap.put("name", "B");

    String result = path.serializeAsPartExpression(stringMap);
    assertThat(result).isEqualTo("A[5].B");
  }

  @Test
  void selectRecursiveElement() {
    assertThat(employeePathCreator.selectEmployees().at(5).selectId().serialize())
        .isEqualTo("employees[5].id");
  }

  @Test
  void selectRecursiveElementAFewLevelDepth() {
    assertThat(
            employeePathCreator
                .selectEmployees()
                .at(1)
                .selectDepartment()
                .selectManager()
                .selectDepartment()
                .selectEmployees()
                .at(5)
                .selectId()
                .serialize())
        .isEqualTo("employees[1].department.manager.department.employees[5].id");
  }

  @Test
  void pathTest() {
    BooksRepository.Paths.Root pathCreator = new BooksRepository.Paths.Root();
    assertThat(pathCreator.selectTitles().at(1).serialize()).isEqualTo("titles[1]");
    assertThat(pathCreator.selectFullAuthorNames().at(1).at(1).serialize())
        .isEqualTo("fullAuthorNames[1][1]");
    assertThat(pathCreator.selectReferences().at(1).serialize()).isEqualTo("references[1]");
    assertThat(pathCreator.selectGridOfBooks().at(0).at(0).serialize())
        .isEqualTo("gridOfBooks[0][0]");
    assertThat(pathCreator.selectGridOfBooks().at(0).at(0).at(0).serialize())
        .isEqualTo("gridOfBooks[0][0][0]");
    assertThat(pathCreator.selectGridOfBooks().at(0).at(0).at(0).selectAuthors().at(1).serialize())
        .isEqualTo("gridOfBooks[0][0][0].authors[1]");
    assertThat(pathCreator.selectFourDimensions().at(0).serialize()).isEqualTo("fourDimensions[0]");
    assertThat(pathCreator.selectFourDimensions().at(0).at(0).serialize())
        .isEqualTo("fourDimensions[0][0]");
    assertThat(pathCreator.selectFourDimensions().at(0).at(0).at(0).serialize())
        .isEqualTo("fourDimensions[0][0][0]");
    assertThat(pathCreator.selectFourDimensions().at(0).at(0).at(0).at(0).serialize())
        .isEqualTo("fourDimensions[0][0][0][0]");
  }
}
