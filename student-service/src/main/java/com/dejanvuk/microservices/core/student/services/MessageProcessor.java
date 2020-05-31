package com.dejanvuk.microservices.core.student.services;

import com.dejanvuk.api.core.student.Student;
import com.dejanvuk.api.core.student.StudentService;
import com.dejanvuk.api.event.Event;
import com.dejanvuk.util.exceptions.EventProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

@EnableBinding(Sink.class)
public class MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);

    private final StudentService studentService;

    @Autowired
    public MessageProcessor(StudentService studentService) {
        this.studentService = studentService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Student> event) {

        LOG.info("Event created at {} ", event.getCreationDate());

        switch (event.getEventType()) {
            case CREATE:
                Student student = event.getData();
                studentService.createStudent(student);
                break;
            case DELETE:
                int universityId = event.getKey();
                studentService.deleteStudents(universityId);
                break;
            default:
                throw new EventProcessingException("Invalid event!");
        }
    }
}
