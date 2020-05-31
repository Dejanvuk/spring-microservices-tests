package com.dejanvuk.microservices.core.university.services;

import com.dejanvuk.api.core.university.University;
import com.dejanvuk.microservices.core.university.persistence.UniversityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.dejanvuk.api.core.university.UniversityService;
import com.dejanvuk.microservices.core.university.persistence.UniversityRepository;
import com.dejanvuk.util.exceptions.InvalidPayloadException;
import com.dejanvuk.util.exceptions.NotFoundException;
import com.dejanvuk.util.http.ServiceUtil;

import java.util.Objects;
import java.util.Random;

import static reactor.core.publisher.Mono.error;


@RestController
public class UniversityServiceImpl implements UniversityService {
    private static final Logger LOG = LoggerFactory.getLogger(UniversityServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final UniversityRepository repository;
    private final UniversityMapper mapper;

    @Autowired
    public UniversityServiceImpl(ServiceUtil serviceUtil, UniversityRepository universityRepository, UniversityMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = universityRepository;
        this.mapper = mapper;
    }

    @Override
    public University createUniversity(University body) {
        Objects.requireNonNull(body);

        if (body.getUniversityId() < 1) throw new InvalidPayloadException("Invalid universityId: " + body.getUniversityId());

        UniversityEntity entity = mapper.universityToUniversityEntity(body);
        Mono<University> newEntity = repository.save(entity)
                .log()
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new InvalidPayloadException("University Id: " + body.getUniversityId() + " already exists!"))
                .map(e -> mapper.universityEntityToUniversity(e));

        return newEntity.block();
    }

    @Override
    public Mono<University> getUniversity(int universityId, int delay, int faultPercent) {
        if (universityId < 1) throw new InvalidPayloadException("Invalid universityId: " + universityId);

        if (delay > 0) {
            try {Thread.sleep(delay * 1000);} catch (InterruptedException e) {}
        }

        if (faultPercent > 0) {
            final Random randomNumberGenerator = new Random();
            int randomThreshold = randomNumberGenerator.nextInt((100 - 1) + 1) + 1;
            if (faultPercent >= randomThreshold) {
                throw new RuntimeException("Random delay");
            }
        }

        return repository.findByUniversityId(universityId)
                .switchIfEmpty(error(new NotFoundException("No University found for universityId: " + universityId)))
                .log()
                .map(e -> mapper.universityEntityToUniversity(e))
                .map(e -> {e.setServiceAddress(serviceUtil.getServiceAddress()); return e;});
    }

    @Override
    public void deleteUniversity(int universityId) {
        if (universityId < 1) throw new InvalidPayloadException("Invalid universityId: " + universityId);

        repository.findByUniversityId(universityId).log().map(e -> repository.delete(e)).flatMap(e -> e).block();

        LOG.debug("Deleted University with universityId: {}", universityId);
    }

}
