package com.dejanvuk.microservices.core.universitycomposite.services;

import com.dejanvuk.api.composite.university.*;
import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.api.core.university.University;
import io.github.resilience4j.reactor.retry.RetryExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.handler.advice.RequestHandlerCircuitBreakerAdvice;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.dejanvuk.util.http.ServiceUtil;

import com.dejanvuk.api.composite.university.ServiceAddresses;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UniversityCompositeServiceImpl implements UniversityCompositeService {
    private final ServiceUtil serviceUtil;
    private UniversityCompositeIntegration integration;
    private final SecurityContext nullSC = new SecurityContextImpl();
    private static final Logger LOG = LoggerFactory.getLogger(UniversityCompositeServiceImpl.class);

    @Autowired
    public UniversityCompositeServiceImpl(ServiceUtil serviceUtil, UniversityCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public Mono<Void> createCompositeUniversity(UniversityAggregate body) {
        return ReactiveSecurityContextHolder.getContext().doOnSuccess(sc -> internalCreateCompositeUniversity(sc, body)).then();
    }

    @Override
    public Mono<UniversityAggregate> getCompositeUniversity(int universityId, int delay, int faultPercent) {
        return Mono.zip(
                values -> createUniversityAggregate((SecurityContext) values[0], (University) values[1], (List<Student>) values[2], serviceUtil.getServiceAddress()),
                ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSC),
                integration.getUniversity(universityId, delay, faultPercent)
                        .onErrorMap(RetryExceptionWrapper.class, retryException -> retryException.getCause())
                        .onErrorReturn(RequestHandlerCircuitBreakerAdvice.CircuitBreakerOpenException.class, getUniversityFallbackValue(universityId)),
                integration.getStudents(universityId).collectList())
                .doOnError(ex -> LOG.warn("GET Composite University fail: {}", ex.toString()))
                .log();
    }

    @Override
    public Mono<Void> deleteCompositeUniversity(int universityId) {
        return ReactiveSecurityContextHolder.getContext().doOnSuccess(sc -> internalDeleteCompositeUniversity(sc, universityId)).then();
    }

    public void internalCreateCompositeUniversity(SecurityContext sc, UniversityAggregate body) {

        try {

            logAuthorizationInfo(sc);

            University university = new University(body.getUniversityId(), body.getName(), body.getCountry(), null);
            integration.createUniversity(university);

            if (body.getStudents() != null) {
                body.getStudents().forEach(r -> {
                    Student student = new Student(body.getUniversityId(), r.getStudentId(),r.getFirstName(), r.getLastName(), r.getSection(), null);
                    integration.createStudent(student);
                });
            }

        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    private University getUniversityFallbackValue(int universityId) {
        return new University(universityId, "Fallback university" + universityId, "Fallback university" + universityId, serviceUtil.getServiceAddress());
    }

    private void internalDeleteCompositeUniversity(SecurityContext sc, int universityId) {
        try {
            logAuthorizationInfo(sc);

            integration.deleteUniversity(universityId);
            integration.deleteStudents(universityId);

            LOG.debug("Deleted University with universityId: {}", universityId);

        } catch (RuntimeException re) {
            throw re;
        }
    }

    private UniversityAggregate createUniversityAggregate(SecurityContext sc, University university, List<Student> students, String serviceAddress) {

        logAuthorizationInfo(sc);

        int universityId = university.getUniversityId();
        String name = university.getName();
        String country = university.getCountry();

        List<StudentSummary> studentSummaries = (students == null)  ? null :
                students.stream()
                        .map(r -> new StudentSummary(r.getStudentId(), r.getFirstName(), r.getLastName(), r.getSection()))
                        .collect(Collectors.toList());

        String universityAddress = university.getServiceAddress();
        String studentAddress = (students != null && students.size() > 0) ? students.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, universityAddress, studentAddress);

        return new UniversityAggregate(universityId, name, country, studentSummaries, serviceAddresses);
    }

    private void logAuthorizationInfo(SecurityContext sc) {
        if (sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken) {
            Jwt jwtToken = ((JwtAuthenticationToken)sc.getAuthentication()).getToken();
            logAuthorizationInfo(jwtToken);
        }
    }

    private void logAuthorizationInfo(Jwt jwt) {
        if (jwt != null) {
            if (LOG.isDebugEnabled()) {
                URL issuer = jwt.getIssuer();
                List<String> audience = jwt.getAudience();
                Object subject = jwt.getClaims().get("sub");
                Object scopes = jwt.getClaims().get("scope");
                Object expires = jwt.getClaims().get("exp");

                LOG.debug("Authorization info: Subject: {}, scopes: {}, expires {}: issuer: {}, audience: {}", subject, scopes, expires, issuer, audience);
            }
        }
    }
}
