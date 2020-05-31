package com.dejanvuk.microservices.core.university;

import com.dejanvuk.api.core.university.University;
import com.dejanvuk.util.exceptions.InvalidPayloadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.dejanvuk.api.event.Event;
import com.dejanvuk.microservices.core.university.persistence.UniversityRepository;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.springframework.http.HttpStatus.*;
import static com.dejanvuk.api.event.Event.Type.CREATE;
import static com.dejanvuk.api.event.Event.Type.DELETE;

import static org.junit.jupiter.api.Assertions.*;


@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.data.mongodb.port: 0", "eureka.client.enabled=false","spring.cloud.config.enabled=false"})
class UniversityServiceApplicationTests {

    @Autowired
    private WebTestClient client;

    @Autowired
    private UniversityRepository repository;

    @Autowired
    private Sink channels;

    private AbstractMessageChannel input = null;

    @BeforeEach
    public void setupDb() {
        input = (AbstractMessageChannel) channels.input();
        repository.deleteAll().block();
    }

    @Test
    public void getUniversityById() {

        int universityId = 1;

        assertNull(repository.findByUniversityId(universityId).block());
        assertEquals(0, (long)repository.count().block());

        sendCreateUniversityEvent(universityId);

        assertNotNull(repository.findByUniversityId(universityId).block());
        assertEquals(1, (long)repository.count().block());

        getAndVerifyUniversity(universityId, OK)
                .jsonPath("$.universityId").isEqualTo(universityId);
    }

    @Test
    public void duplicateError() {

        int universityId = 1;

        assertNull(repository.findByUniversityId(universityId).block());

        sendCreateUniversityEvent(universityId);

        assertNotNull(repository.findByUniversityId(universityId).block());

        try {
            sendCreateUniversityEvent(universityId);
            fail("Expected a MessagingException here!");
        } catch (MessagingException me) {
            if (me.getCause() instanceof InvalidPayloadException)	{
                InvalidPayloadException iie = (InvalidPayloadException)me.getCause();
                assertEquals("University Id: " + universityId + " already exists!", iie.getMessage());
            } else {
                fail("Expected a InvalidInputException as the root cause!");
            }
        }
    }

    @Test
    public void deleteUniversity() {

        int universityId = 1;

        sendCreateUniversityEvent(universityId);
        assertNotNull(repository.findByUniversityId(universityId).block());

        sendDeleteUniversityEvent(universityId);
        assertNull(repository.findByUniversityId(universityId).block());

        sendDeleteUniversityEvent(universityId);
    }

    @Test
    public void getUniversityInvalidParameterString() {

        getAndVerifyUniversity("/no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/university/no-integer")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getUniversityNotFound() {

        int universityIdNotFound = 13;
        getAndVerifyUniversity(universityIdNotFound, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/university/" + universityIdNotFound)
                .jsonPath("$.message").isEqualTo("No University found for universityId: " + universityIdNotFound);
    }

    @Test
    public void getUniversityInvalidParameterNegativeValue() {

        int universityIdInvalid = -1;

        getAndVerifyUniversity(universityIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/university/" + universityIdInvalid)
                .jsonPath("$.message").isEqualTo("Invalid universityId: " + universityIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyUniversity(int universityId, HttpStatus expectedStatus) {
        return getAndVerifyUniversity("/" + universityId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyUniversity(String universityIdPath, HttpStatus expectedStatus) {
        return client.get()
                .uri("/university" + universityIdPath)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateUniversityEvent(int universityId) {
        University university = new University(universityId, "Name " + universityId, "country " + universityId, "sa");
        Event<Integer, University> event = new Event(CREATE, universityId, university);
        input.send(new GenericMessage<>(event));
    }

    private void sendDeleteUniversityEvent(int universityId) {
        Event<Integer, University> event = new Event(DELETE, universityId, null);
        input.send(new GenericMessage<>(event));
    }
}
