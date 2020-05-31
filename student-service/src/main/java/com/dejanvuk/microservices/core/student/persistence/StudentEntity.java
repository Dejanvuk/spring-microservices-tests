package com.dejanvuk.microservices.core.student.persistence;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "students", indexes = { @Index(name = "students_index", unique = true, columnList = "universityId,studentId") })
public class StudentEntity {

    @Id
    @GeneratedValue
    private int id;

    @Version
    private int version;

    private int universityId;
    private int studentId;
    private String firstName;
    private String lastName;
    private String section;

    public StudentEntity() {
    }

    public StudentEntity(int universityId, int studentId, String firstName, String lastName, String section) {
        this.universityId = universityId;
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.section = section;
    }
}

