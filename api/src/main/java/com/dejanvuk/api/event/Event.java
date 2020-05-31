package com.dejanvuk.api.event;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

public class Event<K, T> {

    public enum Type {CREATE, DELETE}

    private Event.Type eventType;
    private K key;
    private T data;
    private LocalDateTime creationDate;

    public Event() {
        this.eventType = null;
        this.key = null;
        this.data = null;
        this.creationDate = null;
    }

    public Event(Type eventType, K key, T data) {
        this.eventType = eventType;
        this.key = key;
        this.data = data;
        this.creationDate = now();
    }

    public Type getEventType() {
        return eventType;
    }

    public K getKey() {
        return key;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }
}
