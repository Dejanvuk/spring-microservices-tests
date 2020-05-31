package com.dejanvuk.api.composite.university;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class StudentSummary {
    private int studentId;
    private String firstName;
    private String lastName;
    private String section;
}
