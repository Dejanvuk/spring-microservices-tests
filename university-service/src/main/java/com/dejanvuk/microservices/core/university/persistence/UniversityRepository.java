package com.dejanvuk.microservices.core.university.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UniversityRepository extends ReactiveCrudRepository<UniversityEntity, String> {
    Mono<UniversityEntity> findByUniversityId(int universityId);
}
