package com.dejanvuk.api.core.student;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Student {
    private int universityId;
    private int studentId;
    private String firstName;
    private String lastName;
    private String section;
    private String serviceAddress;
}
