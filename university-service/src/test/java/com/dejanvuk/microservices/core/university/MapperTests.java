package com.dejanvuk.microservices.core.university;

import com.dejanvuk.api.core.university.University;
import com.dejanvuk.microservices.core.university.persistence.UniversityEntity;
import com.dejanvuk.microservices.core.university.services.UniversityMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

public class MapperTests {
    private UniversityMapper mapper = Mappers.getMapper(UniversityMapper.class);

    @Test
    public void mapperTests() {

        assertNotNull(mapper);

        University university = new University(1, "name", "country", "sa");

        UniversityEntity universityEntity = mapper.universityToUniversityEntity(university);

        assertEquals(university.getUniversityId(), universityEntity.getUniversityId());
        assertEquals(university.getName(), universityEntity.getName());
        assertEquals(university.getCountry(), universityEntity.getCountry());

        University universityEntityToUniversity = mapper.universityEntityToUniversity(universityEntity);

        assertEquals(university.getUniversityId(), universityEntityToUniversity.getUniversityId());
        assertEquals(university.getName(),      universityEntityToUniversity.getName());
        assertEquals(university.getCountry(),    universityEntityToUniversity.getCountry());
        assertNull(universityEntityToUniversity.getServiceAddress());
    }
}
