package com.smartticket.backend.kafka.consumer;

import com.smartticket.backend.kafka.dto.BookingCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BookingEventConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingEventConsumer.class);

    @KafkaListener(
            topics = "booking.created",
            groupId = "smart-ticket-group"
    )
    public void consumeBookingCreatedEvent(BookingCreatedEvent event) {

        logger.info("Received booking event from Kafka");
        logger.info("Event ID: {}", event.getEventId());
        logger.info("Event type: {}", event.getEventType());
        logger.info("Booking ID: {}", event.getBookingId());
        logger.info("User ID: {}", event.getUserId());
        logger.info("Show ID: {}", event.getShowId());
        logger.info("Amount: {}", event.getAmount());
        logger.info("Booking status: {}", event.getBookingStatus());
    }
    @org.springframework.context.event.EventListener(
            org.springframework.kafka.event.ListenerContainerIdleEvent.class
    )
    public void onListenerIdle(
            org.springframework.kafka.event.ListenerContainerIdleEvent event) {
        // Intentionally left empty; this is not a startup event.
    }
}