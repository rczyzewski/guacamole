package io.github.rczyzewski.guacamole.tests;

import org.junit.jupiter.api.Disabled;
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

       assertThat( pathCreator.selectDepartment().selectManager().selectEmployees().at(0).selectName().serialize())
               .isEqualTo("department.manager.employees[0].name");

    }
    @Test
    void validatingPathsWithATopLevelFields(){

        CountryRepository.Paths.CountryPath pathCreator = CountryRepository.Paths.CountryPath.builder().build();
        assertThat(pathCreator.selectId().serialize())
                .isEqualTo("id");

        assertThat(pathCreator.selectName().serialize())
                .isEqualTo("name");

        assertThat(pathCreator.selectHeadOfState().serialize())
                .isEqualTo("PRESIDENT");

        assertThat( pathCreator.selectFullName().serialize())
                .isEqualTo("fullName");

        assertThat( pathCreator.selectPopulation().serialize())
                .isEqualTo("population");

        assertThat( pathCreator.selectFamousPerson().serialize())
                .isEqualTo("famousPerson");

        assertThat( pathCreator.selectFamousMusician().serialize())
                .isEqualTo("ROCK_STAR");

        assertThat( pathCreator.selectArea().serialize())
                .isEqualTo("area");

        assertThat( pathCreator.selectDensity().serialize())
                .isEqualTo("density");
    }

    @Test
    void ensureThatRootPathIsLike(){
        EmployeeRepository.Paths.EmployeePath rp = EmployeeRepository.Paths.EmployeePath.builder().build();
        assertThat(rp.selectId().serialize()).isEqualTo("id");
        assertThat(rp.selectName().serialize()).isEqualTo("name");
        //assertThat(rp.selectTag().serialize()).isEqualTo("tag");
        assertThat(rp.selectEmployees().serialize()).isEqualTo("employees");
        assertThat(rp.selectDepartment().serialize()).isEqualTo("department");
    }

    @Test
    @Disabled("beacause required code is not generated yet")
    void selectStringElementFromList(){
        EmployeeRepository.Paths.EmployeePath rp = EmployeeRepository.Paths.EmployeePath.builder().build();
        /* code like below should be generated
        public ListPath<Employee, TerminalElement<Employee>> selectTag(){

            return ListPath.<Employee, TerminalElement<Employee>>builder()
                    .provider(TerminalElement::new)
                    .selectedField("tag")
                    .parent(this)
                    .build();
        }
         */
        //assertThat(rp.selectTag().at(4).serialize()).isEqualTo("tag[4]");
    }

    @Test
    void selectCompoundElementFromList(){
        EmployeeRepository.Paths.EmployeePath rp = EmployeeRepository.Paths.EmployeePath.builder().build();
        assertThat(rp.selectEmployees().at(5).serialize()).isEqualTo("employees[5]");
        //assertThat(rp.selectEmployees().at(5).selectName().serialize()).isEqualTo("employees[5].name");
    }


    @Test
    void selectRecursiveElement(){
        EmployeeRepository.Paths.EmployeePath rp = EmployeeRepository.Paths.EmployeePath.builder().build();
        assertThat(rp.selectEmployees().at(5).selectId().serialize()).isEqualTo("employees[5].id");
    }

    @Test
    @Disabled("beacause required code is not generated yet")
    void selectManagerOfTheeDepartment(){
        EmployeeRepository.Paths.EmployeePath rp = EmployeeRepository.Paths.EmployeePath.builder().build();
        /*  TODO: code like below should be genrated
        public RootEmployeePath selectManager(){
            PrimitiveElement<Employee> dd = PrimitiveElement.<Employee>builder()
                    .parent(this)
                    .selectedElement("manager")
                    .build();
            return RootEmployeePath.builder().parent(dd).build();
        } */

        rp.selectEmployees()
                .at(1)
                .selectDepartment()
                //.selectManager()
                //.selectDepartment()
                .selectEmployees()
                .at(5)
                .selectId();

        assertThat(rp.selectEmployees().at(5).selectId().serialize()).isEqualTo("employees[5].id");
    }
}
