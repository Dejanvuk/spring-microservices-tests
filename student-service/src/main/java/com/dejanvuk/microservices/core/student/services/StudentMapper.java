package com.dejanvuk.microservices.core.student.services;

import com.dejanvuk.api.core.student.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import com.dejanvuk.microservices.core.student.persistence.StudentEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Student entityToApi(StudentEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    StudentEntity apiToEntity(Student api);

    List<Student> entityListToApiList(List<StudentEntity> entity);
    List<StudentEntity> apiListToEntityList(List<Student> api);
}
