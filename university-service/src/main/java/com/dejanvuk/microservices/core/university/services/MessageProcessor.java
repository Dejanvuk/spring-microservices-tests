package com.dejanvuk.microservices.core.university.services;

import com.dejanvuk.api.core.university.University;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import com.dejanvuk.api.core.university.UniversityService;
import com.dejanvuk.api.event.Event;
import com.dejanvuk.util.exceptions.EventProcessingException;

@EnableBinding(Sink.class)
public class MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);

    private final UniversityService universityService;

    @Autowired
    public MessageProcessor(UniversityService universityService) {
        this.universityService = universityService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, University> event) {

        LOG.info("Event created at {} ", event.getCreationDate());

        switch (event.getEventType()) {
            case CREATE:
                University university = event.getData();
                universityService.createUniversity(university);
                break;
            case DELETE:
                int universityId = event.getKey();
                universityService.deleteUniversity(universityId);
                break;
            default:
                throw new EventProcessingException("Invalid event!");
        }
    }
}
