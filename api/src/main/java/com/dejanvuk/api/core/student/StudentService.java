package com.dejanvuk.api.core.student;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

public interface StudentService {
    @PostMapping(value = "student", produces = "application/json", consumes = "application/json")
    Student createStudent(@RequestBody Student body);

    @GetMapping(value    = "/student", produces = "application/json")
    Flux<Student> getStudents(@RequestParam(value = "universityId", required = true) int universityId);

    @DeleteMapping(value = "student")
    void deleteStudents(@RequestParam(value = "universityId", required = true) int universityId);
}
