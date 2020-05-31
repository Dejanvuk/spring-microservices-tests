package com.dejanvuk.microservices.core.student;


import com.dejanvuk.microservices.core.student.persistence.StudentEntity;
import com.dejanvuk.microservices.core.student.persistence.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@ExtendWith(SpringExtension.class)
@DataJpaTest(properties = {"spring.cloud.config.enabled=false"})
@Transactional(propagation = NOT_SUPPORTED)
public class PersistenceTests {

    @Autowired
    private StudentRepository repository;

    private StudentEntity savedEntity;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll();

        StudentEntity entity = new StudentEntity(1, 2, "firstName", "lastName", "section");
        savedEntity = repository.save(entity);

        assertEqualsStudent(entity, savedEntity);
    }


    @Test
    public void create() {

        StudentEntity newEntity = new StudentEntity(1, 3, "firstName", "lastName", "section");
        repository.save(newEntity);

        StudentEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsStudent(newEntity, foundEntity);

        assertEquals(2, repository.count());
    }

    @Test
    public void update() {
        savedEntity.setFirstName("newFirstName");
        repository.save(savedEntity);

        StudentEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (long) foundEntity.getVersion());
        assertEquals("newFirstName", foundEntity.getFirstName());
    }

    @Test
    public void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    public void getByUniversityId() {
        List<StudentEntity> entityList = repository.findByUniversityId(savedEntity.getUniversityId());

        assertThat(entityList, hasSize(1));
        assertEqualsStudent(savedEntity, entityList.get(0));
    }

    @Test
    public void duplicateError() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            StudentEntity entity = new StudentEntity(1, 2, "firstName", "lastName", "section");
            repository.save(entity);
        });
    }

    @Test
    public void optimisticLockError() {

        // Store the saved entity in two separate entity objects
        StudentEntity entity1 = repository.findById(savedEntity.getId()).get();
        StudentEntity entity2 = repository.findById(savedEntity.getId()).get();

        // Update the entity using the first entity object
        entity1.setFirstName("newFirstName");
        repository.save(entity1);

        //  Update the entity using the second entity object.
        // This should fail since the second entity now holds a old version number, i.e. a Optimistic Lock Error
        try {
            entity2.setFirstName("newFirstName");;
            repository.save(entity2);

            fail("Expected an OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException e) {
        }

        // Get the updated entity from the database and verify its new sate
        StudentEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (int) updatedEntity.getVersion());
        assertEquals("newFirstName", updatedEntity.getFirstName());
    }

    private void assertEqualsStudent(StudentEntity expectedEntity, StudentEntity actualEntity) {
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
        assertEquals(expectedEntity.getUniversityId(), actualEntity.getUniversityId());
        assertEquals(expectedEntity.getStudentId(), actualEntity.getStudentId());
        assertEquals(expectedEntity.getFirstName(), actualEntity.getFirstName());
        assertEquals(expectedEntity.getLastName(), actualEntity.getLastName());
        assertEquals(expectedEntity.getSection(), actualEntity.getSection());
    }
}