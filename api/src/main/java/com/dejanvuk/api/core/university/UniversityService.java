package com.dejanvuk.api.core.university;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface UniversityService {
    @PostMapping(value = "/university", consumes = "application/json", produces = "application/json")
    University createUniversity(@RequestBody University body);

    @GetMapping(value = "/university/{universityId}", produces = "application/json")
    Mono<University> getUniversity(
            @PathVariable int universityId,
            @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
            @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent
    );

    @DeleteMapping(value = "university/{universityId}")
    void deleteUniversity(@PathVariable int universityId);
}
