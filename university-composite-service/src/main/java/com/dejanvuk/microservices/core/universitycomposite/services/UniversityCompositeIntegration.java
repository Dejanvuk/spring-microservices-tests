package com.dejanvuk.microservices.core.universitycomposite.services;

import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.api.core.university.University;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.dejanvuk.api.core.university.UniversityService;
import com.dejanvuk.api.core.student.StudentService;
import com.dejanvuk.api.event.Event;
import com.dejanvuk.util.exceptions.InvalidPayloadException;
import com.dejanvuk.util.exceptions.NotFoundException;
import com.dejanvuk.util.http.HttpErrorInfo;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static reactor.core.publisher.Flux.empty;

import static com.dejanvuk.api.event.Event.Type.CREATE;
import static com.dejanvuk.api.event.Event.Type.DELETE;

@EnableBinding(UniversityCompositeIntegration.MessageSources.class)
@Component
public class UniversityCompositeIntegration implements UniversityService, StudentService {

    private static final Logger LOG = LoggerFactory.getLogger(UniversityCompositeIntegration.class);

    private final String universityServiceUrl = "http://university";
    private final String studentServiceUrl = "http://student";

    private final ObjectMapper mapper;

    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    private final int universityServiceTimeoutSec;

    private MessageSources messageSources;

    public interface MessageSources {

        String OUTPUT_UNIVERSITIES = "output-universities";
        String OUTPUT_STUDENTS = "output-students";

        @Output(OUTPUT_UNIVERSITIES)
        MessageChannel outputUniversities();

        @Output(OUTPUT_STUDENTS)
        MessageChannel outputStudents();
    }

    public UniversityCompositeIntegration(MessageSources messageSources,
                                          WebClient.Builder webClientBuilder,
                                          ObjectMapper mapper,
                                          @Value("${app.university-service.timeoutSec}") int universityServiceTimeoutSec) {
        this.webClientBuilder = webClientBuilder;
        this.mapper = mapper;
        this.messageSources = messageSources;
        this.universityServiceTimeoutSec = universityServiceTimeoutSec;
    }

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder.build();
        }
        return webClient;
    }

    @Override
    public University createUniversity(University body) {
        messageSources.outputUniversities().send(MessageBuilder.withPayload(new Event(CREATE, body.getUniversityId(), body)).build());
        return body;
    }

    @Retry(name = "university")
    @CircuitBreaker(label = "university")
    @Override
    public Mono<University> getUniversity(int universityId, int delay, int faultPercent) {
        URI url = UriComponentsBuilder.fromUriString(universityServiceUrl + "/university/{universityId}?delay={delay}&faultPercent={faultPercent}").build(universityId, delay, faultPercent);

        return getWebClient().get().uri(url).retrieve().bodyToMono(University.class).log().onErrorMap(WebClientResponseException.class, ex -> handleException(ex))
                .timeout(Duration.ofSeconds(universityServiceTimeoutSec));
    }

    @Override
    public void deleteUniversity(int universityId) {
        messageSources.outputUniversities().send(MessageBuilder.withPayload(new Event(DELETE, universityId, null)).build());
    }

    @Override
    public Student createStudent(Student body) {
        messageSources.outputStudents().send(MessageBuilder.withPayload(new Event(CREATE, body.getUniversityId(), body)).build());
        return body;
    }

    @Override
    public Flux<Student> getStudents(int universityId) {
        String url = studentServiceUrl + "/student?universityId=" + universityId;

        return getWebClient().get().uri(url).retrieve().bodyToFlux(Student.class).log().onErrorResume(error -> empty());
    }

    @Override
    public void deleteStudents(int universityId) {
        messageSources.outputStudents().send(MessageBuilder.withPayload(new Event(DELETE, universityId, null)).build());
    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException)ex;

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY :
                return new InvalidPayloadException(getErrorMessage(wcre));

            default:
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
