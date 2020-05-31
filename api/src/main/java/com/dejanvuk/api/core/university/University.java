package com.dejanvuk.api.core.university;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class University {
    private int universityId;
    private String name;
    private String country;
    private String serviceAddress;
}
