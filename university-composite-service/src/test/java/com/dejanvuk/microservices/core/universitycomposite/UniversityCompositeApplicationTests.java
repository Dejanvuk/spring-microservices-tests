package com.dejanvuk.microservices.core.universitycomposite;

import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.api.core.university.University;
import com.dejanvuk.microservices.core.universitycomposite.services.UniversityCompositeIntegration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.dejanvuk.api.composite.university.UniversityAggregate;
import com.dejanvuk.api.composite.university.StudentSummary;
import com.dejanvuk.util.exceptions.InvalidPayloadException;
import com.dejanvuk.util.exceptions.NotFoundException;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import static reactor.core.publisher.Mono.just;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment=RANDOM_PORT,
        classes = {UniversityCompositeApplication.class, TestSecurityConfig.class },
        properties = {"spring.main.allow-bean-definition-overriding=true","eureka.client.enabled=false","spring.cloud.config.enabled=false"})
public class UniversityCompositeApplicationTests {

    private static final int UNIVERSITY_ID_OK = 1;
    private static final int UNIVERSITY_ID_NOT_FOUND = 2;
    private static final int UNIVERSITY_ID_INVALID = 3;

    @Autowired
    private WebTestClient client;

    @MockBean
    private UniversityCompositeIntegration compositeIntegration;

    @BeforeEach
    public void setUp() {

        when(compositeIntegration.getUniversity(eq(UNIVERSITY_ID_OK), anyInt(), anyInt())).
                thenReturn(Mono.just(new University(UNIVERSITY_ID_OK, "name", "country", "mock-address")));

        when(compositeIntegration.getStudents(UNIVERSITY_ID_OK)).
                thenReturn(Flux.fromIterable(singletonList(new Student(UNIVERSITY_ID_OK, 1, "author", "subject", "content", "mock address"))));

        when(compositeIntegration.getUniversity(eq(UNIVERSITY_ID_NOT_FOUND), anyInt(), anyInt())).thenThrow(new NotFoundException("NOT FOUND: " + UNIVERSITY_ID_NOT_FOUND));

        when(compositeIntegration.getUniversity(eq(UNIVERSITY_ID_INVALID), anyInt(), anyInt())).thenThrow(new InvalidPayloadException("INVALID: " + UNIVERSITY_ID_INVALID));
    }
        @Test
    public void contextLoads() {
    }

    @Test
    public void createCompositeUniversity1() {

        UniversityAggregate compositeUniversity = new UniversityAggregate(1, "name", "country", null, null);

        postAndVerifyUniversity(compositeUniversity, OK);
    }

    @Test
    public void createCompositeUniversity2() {
        UniversityAggregate compositeUniversity = new UniversityAggregate(1, "name", "country",
                singletonList(new StudentSummary(1, "a", "s", "c")), null);

        postAndVerifyUniversity(compositeUniversity, OK);
    }

    @Test
    public void deleteCompositeUniversity() {
        UniversityAggregate compositeUniversity = new UniversityAggregate(1, "name", "country",
                singletonList(new StudentSummary(1, "a", "s", "c")), null);

        postAndVerifyUniversity(compositeUniversity, OK);

        deleteAndVerifyUniversity(compositeUniversity.getUniversityId(), OK);
        deleteAndVerifyUniversity(compositeUniversity.getUniversityId(), OK);
    }

    @Test
    public void getUniversityById() {

        getAndVerifyUniversity(UNIVERSITY_ID_OK, OK)
                .jsonPath("$.universityId").isEqualTo(UNIVERSITY_ID_OK)
                .jsonPath("$.students.length()").isEqualTo(1);
    }

    @Test
    public void getUniversityNotFound() {

        getAndVerifyUniversity(UNIVERSITY_ID_NOT_FOUND, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/university-composite/" + UNIVERSITY_ID_NOT_FOUND)
                .jsonPath("$.message").isEqualTo("NOT FOUND: " + UNIVERSITY_ID_NOT_FOUND);
    }

    @Test
    public void getUniversityInvalidInput() {

        getAndVerifyUniversity(UNIVERSITY_ID_INVALID, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/university-composite/" + UNIVERSITY_ID_INVALID)
                .jsonPath("$.message").isEqualTo("INVALID: " + UNIVERSITY_ID_INVALID);
    }

    private WebTestClient.BodyContentSpec getAndVerifyUniversity(int universityId, HttpStatus expectedStatus) {
        return client.get()
                .uri("/university-composite/" + universityId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void postAndVerifyUniversity(UniversityAggregate compositeUniversity, HttpStatus expectedStatus) {
        client.post()
                .uri("/university-composite")
                .body(just(compositeUniversity), UniversityAggregate.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyUniversity(int universityId, HttpStatus expectedStatus) {
        client.delete()
                .uri("/university-composite/" + universityId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}