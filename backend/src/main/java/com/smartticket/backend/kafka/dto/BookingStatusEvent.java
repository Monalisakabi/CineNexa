package com.smartticket.backend.kafka.dto;

import java.time.LocalDateTime;

public class BookingStatusEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long bookingId;
    private Long userId;
    private String bookingStatus;
    private String description;

    public BookingStatusEvent() {
    }

    public BookingStatusEvent(
            String eventId,
            String eventType,
            LocalDateTime timestamp,
            Long bookingId,
            Long userId,
            String bookingStatus,
            String description) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.bookingId = bookingId;
        this.userId = userId;
        this.bookingStatus = bookingStatus;
        this.description = description;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}