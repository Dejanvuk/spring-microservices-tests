package com.dejanvuk.microservices.core.student.services;

import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.api.core.student.StudentService;
import com.dejanvuk.util.exceptions.InvalidPayloadException;
import com.dejanvuk.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import com.dejanvuk.microservices.core.student.persistence.StudentEntity;
import com.dejanvuk.microservices.core.student.persistence.StudentRepository;


import java.util.List;
import java.util.Objects;

@RestController
public class StudentServiceImpl implements StudentService {
    private static final Logger LOG = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final StudentMapper mapper;
    private final StudentRepository repository;
    private final Scheduler scheduler;

    @Autowired
    public StudentServiceImpl(ServiceUtil serviceUtil, StudentMapper mapper, StudentRepository repository, Scheduler scheduler) {
        this.serviceUtil = serviceUtil;
        this.mapper = mapper;
        this.repository = repository;
        this.scheduler = scheduler;
    }

    @Override
    public Student createStudent(Student body) {
        Objects.requireNonNull(body);

        try {
            StudentEntity entity = mapper.apiToEntity(body);
            StudentEntity newEntity = repository.save(entity);

            return mapper.entityToApi(newEntity);

        } catch (DataIntegrityViolationException dive) {
            throw new InvalidPayloadException("The student with universityId: " + body.getUniversityId() + " and studentId:" + body.getStudentId() + "already exists!");
        }
    }

    @Override
    public Flux<Student> getStudents(int universityId) {
        if (universityId < 1) throw new InvalidPayloadException("Invalid universityId: " + universityId);

        return asyncFlux(getByUniversityId(universityId)).log();
    }

    @Override
    public void deleteStudents(int universityId) {
        repository.deleteAll(repository.findByUniversityId(universityId));
    }

    protected List<Student> getByUniversityId(int universityId) {

        List<StudentEntity> entityList = repository.findByUniversityId(universityId);
        List<Student> list = mapper.entityListToApiList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

        return list;
    }

    private <T> Flux<T> asyncFlux(Iterable<T> iterable) {
        return Flux.fromIterable(iterable).publishOn(scheduler);
    }

}
