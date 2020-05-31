package com.dejanvuk.microservices.core.university.services;

import com.dejanvuk.api.core.university.University;
import com.dejanvuk.microservices.core.university.persistence.UniversityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UniversityMapper {

    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    University universityEntityToUniversity(UniversityEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    UniversityEntity universityToUniversityEntity(University api);
}
