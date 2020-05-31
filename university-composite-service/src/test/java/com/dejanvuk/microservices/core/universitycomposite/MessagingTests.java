package com.dejanvuk.microservices.core.universitycomposite;


import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.api.core.university.University;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.dejanvuk.api.composite.university.UniversityAggregate;
import com.dejanvuk.api.composite.university.StudentSummary;
import com.dejanvuk.api.event.Event;
import com.dejanvuk.microservices.core.universitycomposite.services.UniversityCompositeIntegration;

import java.util.concurrent.BlockingQueue;

import static java.util.Collections.singletonList;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static org.springframework.http.HttpStatus.OK;

import static org.junit.jupiter.api.Assertions.*;

import static reactor.core.publisher.Mono.just;
import static com.dejanvuk.api.event.Event.Type.CREATE;
import static com.dejanvuk.api.event.Event.Type.DELETE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static com.dejanvuk.microservices.core.universitycomposite.IsSameEvent.sameEventExceptCreatedAt;


@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment=RANDOM_PORT,
        classes = {UniversityCompositeApplication.class, TestSecurityConfig.class },
        properties = {"spring.main.allow-bean-definition-overriding=true","eureka.client.enabled=false","spring.cloud.config.enabled=false"})
public class MessagingTests {

    private static final int UNIVERSITY_ID_OK = 1;
    private static final int UNIVERSITY_ID_NOT_FOUND = 2;
    private static final int UNIVERSITY_ID_INVALID = 3;

    @Autowired
    private WebTestClient client;

    @Autowired
    private UniversityCompositeIntegration.MessageSources channels;

    @Autowired
    private MessageCollector collector;



    BlockingQueue<Message<?>> queueUniversities = null;
    BlockingQueue<Message<?>> queueStudents = null;

    @BeforeEach
    public void setUp() {
        queueUniversities = getQueue(channels.outputUniversities());
        queueStudents = getQueue(channels.outputStudents());
    }

    @Test
    public void createCompositeUniversity1() {

        UniversityAggregate composite = new UniversityAggregate(1, "name", "country", null, null);
        postAndVerifyUniversity(composite, OK);

        assertEquals(1, queueUniversities.size());

        Event<Integer, University> expectedEvent = new Event(CREATE, composite.getUniversityId(), new University(composite.getUniversityId(), composite.getName(), composite.getCountry(), null));
        assertThat(queueUniversities, is(receivesPayloadThat(sameEventExceptCreatedAt(expectedEvent))));

        assertEquals(0, queueStudents.size());
    }

    @Test
    public void createCompositeUniversity2() {

        UniversityAggregate composite = new UniversityAggregate(1, "name", "country",
                singletonList(new StudentSummary(1, "a", "s", "c")), null);

        postAndVerifyUniversity(composite, OK);

        assertEquals(1, queueUniversities.size());

        Event<Integer, University> expectedUniversityEvent = new Event(CREATE, composite.getUniversityId(), new University(composite.getUniversityId(), composite.getName(), composite.getCountry(), null));
        assertThat(queueUniversities, receivesPayloadThat(sameEventExceptCreatedAt(expectedUniversityEvent)));

        assertEquals(1, queueStudents.size());

        StudentSummary studentSummary = composite.getStudents().get(0);
        Event<Integer, University> expectedStudentEvent = new Event(CREATE, composite.getUniversityId(), new Student(composite.getUniversityId(), studentSummary.getStudentId(), studentSummary.getFirstName(), studentSummary.getLastName(), studentSummary.getSection(), null));
        assertThat(queueStudents, receivesPayloadThat(sameEventExceptCreatedAt(expectedStudentEvent)));
    }

    @Test
    public void deleteCompositeUniversity() {

        deleteAndVerifyUniversity(1, OK);

        assertEquals(1, queueUniversities.size());

        Event<Integer, University> expectedEvent = new Event(DELETE, 1, null);
        assertThat(queueUniversities, is(receivesPayloadThat(sameEventExceptCreatedAt(expectedEvent))));

        assertEquals(1, queueStudents.size());

        Event<Integer, University> expectedStudentEvent = new Event(DELETE, 1, null);
        assertThat(queueStudents, receivesPayloadThat(sameEventExceptCreatedAt(expectedStudentEvent)));
    }

    private BlockingQueue<Message<?>> getQueue(MessageChannel messageChannel) {
        return collector.forChannel(messageChannel);
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
