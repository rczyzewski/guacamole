package io.github.rczyzewski.testing_expressions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PathTest{
    @Test
    void validatingPaths(){

        EmployeeRepository.Paths.EmployeePath pathCreator = EmployeeRepository.Paths.EmployeePath.builder().build();
        assertThat(pathCreator.selectEmployees().serialize())
                .isEqualTo("employees");

        assertThat(pathCreator.selectEmployees().at(2).serialize())
                .isEqualTo("employees[2]");

        assertThat(pathCreator.selectEmployees().at(2).selectEmployees().serialize())
                .isEqualTo("employees[2].employees");
        assertThat(
                pathCreator.selectDepartment().selectEmployees().at(1).selectEmployees().serialize())
                .isEqualTo("department.employees[1].employees");

       assertThat( pathCreator.selectDepartment().selectManaager().selectEmployees().at(0).selectName().serialize())
               .isEqualTo("department.manaager.employees[0].name");

    }
}
