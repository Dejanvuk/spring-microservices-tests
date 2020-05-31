package com.dejanvuk.api.composite.university;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface UniversityCompositeService {

    @PostMapping(
            value    = "/university-composite",
            consumes = "application/json")
    Mono<Void> createCompositeUniversity(@RequestBody UniversityAggregate body);

    @GetMapping(value = "university-composite/{universityId}" , produces = "application/json")
    Mono<UniversityAggregate> getCompositeUniversity(
            @PathVariable int universityId,
            @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
            @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent
    );

    @DeleteMapping(value = "/university-composite/{universityId}")
    Mono<Void> deleteCompositeUniversity(@PathVariable int universityId);
}
