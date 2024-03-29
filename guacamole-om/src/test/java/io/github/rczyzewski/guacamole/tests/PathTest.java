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

  EmployeeRepository.Paths.Root pathCreator = new EmployeeRepository.Paths.Root();

  @Test
  void validatingPaths() {

    assertThat(pathCreator.selectEmployees().serialize()).isEqualTo("employees");

    assertThat(pathCreator.selectEmployees().at(2).serialize()).isEqualTo("employees[2]");

    assertThat(pathCreator.selectEmployees().at(2).selectEmployees().serialize())
        .isEqualTo("employees[2].employees");
    assertThat(pathCreator.selectDepartment().selectEmployees().at(1).selectEmployees().serialize())
        .isEqualTo("department.employees[1].employees");

    assertThat(
            pathCreator
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

    CountryRepository.Paths.CountryPath pathCreator =
        CountryRepository.Paths.CountryPath.builder().build();
    assertThat(pathCreator.selectId().serialize()).isEqualTo("id");

    assertThat(pathCreator.selectName().serialize()).isEqualTo("name");

    assertThat(pathCreator.selectHeadOfState().serialize()).isEqualTo("PRESIDENT");

    assertThat(pathCreator.selectFullName().serialize()).isEqualTo("fullName");

    assertThat(pathCreator.selectPopulation().serialize()).isEqualTo("population");

    assertThat(pathCreator.selectFamousPerson().serialize()).isEqualTo("famousPerson");

    assertThat(pathCreator.selectFamousMusician().serialize()).isEqualTo("ROCK_STAR");

    assertThat(pathCreator.selectArea().serialize()).isEqualTo("area");

    assertThat(pathCreator.selectDensity().serialize()).isEqualTo("density");
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
    assertThat(pathCreator.selectEmployees().at(5).serialize()).isEqualTo("employees[5]");
    assertThat(pathCreator.selectEmployees().at(5).selectName().serialize())
        .isEqualTo("employees[5].name");
  }

  @Test
  void ensureThatPathPartsAreGeneratedCorrectly() {
    Path<Employee> path = pathCreator.selectEmployees().at(5).selectName();

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
    assertThat(pathCreator.selectEmployees().at(5).selectId().serialize())
        .isEqualTo("employees[5].id");
  }

  @Test
  void selectRecursiveElementAFewLevelDepth() {
    assertThat(
            pathCreator
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
}
