package com.dejanvuk.microservices.core.university.persistence;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Document(collection="universities")
public class UniversityEntity {

    @Id
    private String id;

    @Version
    private Integer version;

    @Indexed(unique = true)
    private int universityId;


    private String name;
    private String country;

    public UniversityEntity(int universityId, String name, String country) {
        this.universityId = universityId;
        this.name = name;
        this.country = country;
    }

}