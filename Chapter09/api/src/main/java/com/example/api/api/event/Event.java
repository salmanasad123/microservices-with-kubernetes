package com.example.api.api.event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

/**
 * Messaging systems handle messages that typically consist of headers and a body. An event is a message
 * that describes something that has happened. For events, the message body can be used to describe
 * the type of event, the event data, and a timestamp for when the event occurred.
 *  An event is, for the scope of this book, defined by the following:
 *  • The type of event, for example, a create or delete event
 *  • A key that identifies the data, for example, a product ID
 *  • A data element, that is, the actual data in the event
 *  • A timestamp, which describes when the event occurred
 * @param <K>
 * @param <T>
 */
public class Event<K,T> {

    // The event type is declared as an enumerator with the allowed values, that is, CREATE and DELETE.
    public enum Type {
        CREATE,
        DELETE
    }
    private final Type eventType;
    private final K key;
    private final T data;
    private final ZonedDateTime eventCreatedAt;

    public Event() {
        this.eventType = null;
        this.key = null;
        this.data = null;
        this.eventCreatedAt = null;
    }

    public Event(Type eventType, K key, T data) {
        this.eventType = eventType;
        this.key = key;
        this.data = data;
        this.eventCreatedAt = now();
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

    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    public ZonedDateTime getEventCreatedAt() {
        return eventCreatedAt;
    }

}
