package com.dejanvuk.microservices.core.student;

import com.dejanvuk.api.core.university.University;
import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.api.event.Event;
import com.dejanvuk.microservices.core.student.persistence.StudentRepository;
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


import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static com.dejanvuk.api.event.Event.Type.*;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.http.HttpStatus.*;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:student-db", "eureka.client.enabled=false","spring.cloud.config.enabled=false"})
class StudentApplicationTests {

    @Autowired
    private WebTestClient client;

    @Autowired
    private StudentRepository repository;

    @Autowired
    private Sink channels;

    private AbstractMessageChannel input = null;

    @BeforeEach
    public void setupDb() {
        input = (AbstractMessageChannel) channels.input();
        repository.deleteAll();
    }

    @Test
    public void getStudentsByUniversityId() {

        int universityId = 1;

        assertEquals(0, repository.findByUniversityId(universityId).size());

        sendCreateStudentEvent(universityId, 1);
        sendCreateStudentEvent(universityId, 2);
        sendCreateStudentEvent(universityId, 3);

        assertEquals(3, repository.findByUniversityId(universityId).size());

        getAndVerifyStudentsByUniversityId(universityId, OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].universityId").isEqualTo(universityId)
                .jsonPath("$[2].studentId").isEqualTo(3);
    }

    @Test
    public void duplicateError() {

        int universityId = 1;
        int studentId = 1;

        assertEquals(0, repository.count());

        sendCreateStudentEvent(universityId, studentId);

        assertEquals(1, repository.count());

        try {
            sendCreateStudentEvent(universityId, studentId);
            fail("Expected a MessagingException here!");
        } catch (MessagingException me) {
            if (me.getCause() instanceof InvalidPayloadException)	{
                InvalidPayloadException iie = (InvalidPayloadException)me.getCause();
                assertEquals("The student with universityId: " + universityId +  " and studentId:" + studentId + "already exists!" , iie.getMessage());
            } else {
                fail("Expected a InvalidInputException as the root cause!");
            }
        }

        assertEquals(1, repository.count());
    }

    @Test
    public void deleteStudents() {

        int universityId = 1;
        int studentId = 1;

        sendCreateStudentEvent(universityId, studentId);
        assertEquals(1, repository.findByUniversityId(universityId).size());

        sendDeleteStudentEvent(universityId);
        assertEquals(0, repository.findByUniversityId(universityId).size());

        sendDeleteStudentEvent(universityId);
    }

    @Test
    public void getStudentsMissingParameter() {

        getAndVerifyStudentsByUniversityId("", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/student")
                .jsonPath("$.message").isEqualTo("Required int parameter 'universityId' is not present");
    }

    @Test
    public void getStudentInvalidParameter() {

        getAndVerifyStudentsByUniversityId("?universityId=no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/student")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getStudentsNotFound() {

        getAndVerifyStudentsByUniversityId("?universityId=213", OK)
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void getStudentsInvalidParameterNegativeValue() {

        int universityIdInvalid = -1;

        getAndVerifyStudentsByUniversityId("?universityId=" + universityIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/student")
                .jsonPath("$.message").isEqualTo("Invalid universityId: " + universityIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyStudentsByUniversityId(int universityId, HttpStatus expectedStatus) {
        return getAndVerifyStudentsByUniversityId("?universityId=" + universityId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyStudentsByUniversityId(String universityIdQuery, HttpStatus expectedStatus) {
        return client.get()
                .uri("/student" + universityIdQuery)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateStudentEvent(int universityId, int studentId) {
        Student student = new Student(universityId, studentId, "firstName " + studentId, "lastName " + studentId, "section" + studentId, "SA");
        Event<Integer, University> event = new Event(CREATE, universityId, student);
        input.send(new GenericMessage<>(event));
    }

    private void sendDeleteStudentEvent(int universityId) {
        Event<Integer, University> event = new Event(DELETE, universityId, null);
        input.send(new GenericMessage<>(event));
    }
}
