package com.dejanvuk.microservices.core.student;

import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.microservices.core.student.persistence.StudentEntity;
import com.dejanvuk.microservices.core.student.services.StudentMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MapperTests {
    private StudentMapper mapper = Mappers.getMapper(StudentMapper.class);


    @Test
    public void mapperTests() {

        assertNotNull(mapper);

        Student api = new Student(1, 2, "firstName","lastName","section","sa");

        StudentEntity entity = mapper.apiToEntity(api);

        assertEquals(api.getUniversityId(), entity.getUniversityId());
        assertEquals(api.getStudentId(), entity.getStudentId());
        assertEquals(api.getFirstName(), entity.getFirstName());
        assertEquals(api.getLastName(), entity.getLastName());
        assertEquals(api.getSection(), entity.getSection());

        Student studentFromEntity = mapper.entityToApi(entity);

        assertEquals(studentFromEntity.getUniversityId(), api.getUniversityId());
        assertEquals(studentFromEntity.getStudentId(), api.getStudentId());
        assertEquals(studentFromEntity.getFirstName(), api.getFirstName());
        assertEquals(studentFromEntity.getLastName(), api.getLastName());
        assertEquals(studentFromEntity.getSection(), api.getSection());
    }

    @Test
    public void mapperListTests() {

        assertNotNull(mapper);

        Student api = new Student(1, 2, "firstName","lastName","section","sa");
        List<Student> apiList = Collections.singletonList(api);

        List<StudentEntity> entityList = mapper.apiListToEntityList(apiList);
        assertEquals(apiList.size(), entityList.size());

        StudentEntity entity = entityList.get(0);

        assertEquals(api.getUniversityId(), entity.getUniversityId());
        assertEquals(api.getStudentId(), entity.getStudentId());
        assertEquals(api.getFirstName(), entity.getFirstName());
        assertEquals(api.getLastName(), entity.getLastName());
        assertEquals(api.getSection(), entity.getSection());

        List<Student> studentFromEntityList = mapper.entityListToApiList(entityList);
        assertEquals(apiList.size(), studentFromEntityList.size());

        Student studentFromEntity = studentFromEntityList.get(0);

        assertEquals(studentFromEntity.getUniversityId(), api.getUniversityId());
        assertEquals(studentFromEntity.getStudentId(), api.getStudentId());
        assertEquals(studentFromEntity.getFirstName(), api.getFirstName());
        assertEquals(studentFromEntity.getLastName(), api.getLastName());
        assertEquals(studentFromEntity.getSection(), api.getSection());
        assertNull(studentFromEntity.getServiceAddress());
    }
}
