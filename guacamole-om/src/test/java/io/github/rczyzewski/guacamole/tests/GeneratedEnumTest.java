package io.github.rczyzewski.guacamole.tests;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeneratedEnumTest {
    @Test
    void checkAllStringsGeneratedEnum(){
        assertThat(CountryRepository.AllStrings.NAME.getDdbField()).isEqualTo("name");
        assertThat(CountryRepository.AllStrings.HEAD_OF_STATE.getDdbField()).isEqualTo("PRESIDENT");
        assertThat(CountryRepository.AllStrings.FAMOUS_MUSICIAN.getDdbField()).isEqualTo("ROCK_STAR");
        assertThat(CountryRepository.AllStrings.ID.getDdbField()).isEqualTo("id");
        assertThat(CountryRepository.AllStrings.NAME.getDdbField()).isEqualTo("name");
        assertThat(CountryRepository.AllStrings.FULL_NAME.getDdbField()).isEqualTo("fullName");
    }
    @Test
    void checkAllFieldsGeneratedEnum(){
        assertThat(CountryRepository.AllFields.NAME.getDdbField()).isEqualTo("name");
        assertThat(CountryRepository.AllFields.HEAD_OF_STATE.getDdbField()).isEqualTo("PRESIDENT");
        assertThat(CountryRepository.AllFields.FAMOUS_MUSICIAN.getDdbField()).isEqualTo("ROCK_STAR");
        assertThat(CountryRepository.AllFields.ID.getDdbField()).isEqualTo("id");
        assertThat(CountryRepository.AllFields.NAME.getDdbField()).isEqualTo("name");
        assertThat(CountryRepository.AllFields.FULL_NAME.getDdbField()).isEqualTo("fullName");
        assertThat(CountryRepository.AllFields.AREA.getDdbField()).isEqualTo("area");
        assertThat(CountryRepository.AllFields.DENSITY.getDdbField()).isEqualTo("density");
        assertThat(CountryRepository.AllFields.POPULATION.getDdbField()).isEqualTo("population");

    }
}
