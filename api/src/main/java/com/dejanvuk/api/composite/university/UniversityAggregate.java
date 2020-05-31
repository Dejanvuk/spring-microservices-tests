package com.dejanvuk.api.composite.university;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class UniversityAggregate {
    private int universityId;
    private String name;
    private String country;
    private List<StudentSummary> students;
    private ServiceAddresses serviceAddresses;
}
