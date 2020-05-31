package com.dejanvuk.microservices.core.student.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StudentRepository extends CrudRepository<StudentEntity, Integer> {

    @Transactional(readOnly = true)
    List<StudentEntity> findByUniversityId(int productId);
}