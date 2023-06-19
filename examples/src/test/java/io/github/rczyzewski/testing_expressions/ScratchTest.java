package io.github.rczyzewski.testing_expressions;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBDocument;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBHashKey;
import io.github.rczyzewski.guacamole.ddb.datamodeling.DynamoDBTable;
import io.github.rczyzewski.guacamole.ddb.path.ListPath;
import io.github.rczyzewski.guacamole.ddb.path.Path;
import io.github.rczyzewski.guacamole.ddb.path.PrimitiveElement;
import io.github.rczyzewski.guacamole.ddb.path.TerminalElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ScratchTest{

    @Test
    void ensureThatRootPathisLike(){
        RootEmployeePath rp = new RootEmployeePath(null);
        assertThat(rp.selectId().serialize()).isEqualTo("id");
        assertThat(rp.selectName().serialize()).isEqualTo("name");
        assertThat(rp.selectTag().serialize()).isEqualTo("tag");
        assertThat(rp.selectEmployees().serialize()).isEqualTo("employees");
        assertThat(rp.selectDepartment().serialize()).isEqualTo("department");
    }

    @Test
    void selectStringElementFromList(){
        RootEmployeePath rp = new RootEmployeePath();
        assertThat(rp.selectTag().at(4).serialize()).isEqualTo("tag[4]");
    }

    @Test
    void selectCompoundElementFromList(){
        RootEmployeePath rp = new RootEmployeePath();
        assertThat(rp.selectEmployees().at(5).serialize()).isEqualTo("employees[5]");
        //assertThat(rp.selectEmployees().at(5).selectName().serialize()).isEqualTo("employees[5].name");
    }


    @Test
    void selectRecursiveElement(){
        RootEmployeePath rp = new RootEmployeePath();
        assertThat(rp.selectEmployees().at(5).selectId().serialize()).isEqualTo("employees[5].id");
    }

    @Test
    void selectManagerOfTheeDepartment(){
        RootEmployeePath rp = new RootEmployeePath();
        rp.selectEmployees()
          .at(1)
          .selectDepartment()
          .selectManager()
          .selectDepartment()
          .selectEmployees()
          .at(5)
          .selectId();

        assertThat(rp.selectEmployees().at(5).selectId().serialize()).isEqualTo("employees[5].id");
    }
}

@Value
@Builder
@DynamoDBTable
@With
class Employee{
    @DynamoDBHashKey
    String id;
    String name;
    List<String> tags;
    List<Employee> employees;
    Department department;
}

@Value
@Builder
@With
@DynamoDBDocument
class Department{
    String id;
    String name;
    String location;
    Employee manaager;
    List<Employee> employees;
}



@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
class RootEmployeePath implements Path<Employee>{

    Path<Employee> parent;

    @Override
    public String serialize(){
        return Optional.ofNullable(parent)
                       .map(it -> parent.serialize()).orElse(null);
    }

    public Path<Employee> selectId(){
        return PrimitiveElement.<Employee>builder().parent(this).selectedElement("id").build();
    }

    public Path<Employee> selectName(){
        return PrimitiveElement.<Employee>builder().parent(this).selectedElement("name").build();
    }

    public DepartmentEmloyeePath selectDepartment(){
        PrimitiveElement<Employee> dd = PrimitiveElement.<Employee>builder()
                                                        .parent(this)
                                                        .selectedElement("department")
                                                        .build();
        return DepartmentEmloyeePath.builder().parent(dd).build();
    }

    public ListPath<Employee, TerminalElement<Employee>> selectTag(){

        return ListPath.<Employee, TerminalElement<Employee>>builder()
                       .provider(TerminalElement::new)
                       .selectedField("tag")
                       .parent(this)
                       .build();
    }

    public ListPath<Employee, RootEmployeePath> selectEmployees(){
        return ListPath.<Employee, RootEmployeePath>builder()
                       .provider(RootEmployeePath::new)
                       .selectedField("employees")
                       .parent(this)
                       .build();

    }
}

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
class DepartmentEmloyeePath implements Path<Employee>{

    Path<Employee> parent;

    public Path<Employee> selectId(){
        return PrimitiveElement.<Employee>builder().parent(this).selectedElement("id").build();
    }

    public Path<Employee> selectName(){
        return PrimitiveElement.<Employee>builder().parent(this).selectedElement("name").build();
    }
    public Path<Employee> selectLocation(){
        return PrimitiveElement.<Employee>builder().parent(this).selectedElement("location").build();
    }

    public RootEmployeePath selectManager(){
        PrimitiveElement<Employee> dd = PrimitiveElement.<Employee>builder()
                                                        .parent(this)
                                                        .selectedElement("manager")
                                                        .build();
        return RootEmployeePath.builder().parent(dd).build();
    }
    public ListPath<Employee, RootEmployeePath> selectEmployees(){
        return ListPath.<Employee, RootEmployeePath>builder()
                       .provider(RootEmployeePath::new)
                       .selectedField("employees")
                       .parent(this)
                       .build();

    }

    @Override
    public String serialize(){
        return Optional.ofNullable(parent)
                       .map(it -> parent.serialize()).orElse(null);
    }

}
