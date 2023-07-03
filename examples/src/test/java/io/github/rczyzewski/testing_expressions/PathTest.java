package io.github.rczyzewski.testing_expressions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PathTest{
    @Test
    void validatingPaths(){

        EmployeeRepository.Paths.EmployeePath pathCreator = EmployeeRepository.Paths.EmployeePath.builder().build();

        assertThat(pathCreator.selectEmployees().at(2).selectEmployees().serialize())
                .isEqualTo("employees[2].employees");
        assertThat(
                pathCreator.selectDepartment().selectEmployees().at(1).selectEmployees().serialize())
                .isEqualTo("department.emplyees[1].employees");

       assertThat( pathCreator.selectDepartment().selectManaager().selectEmployees().at(0).selectName().serialize())
               .isEqualTo("department.manager.employees[0].name");

    }
}
