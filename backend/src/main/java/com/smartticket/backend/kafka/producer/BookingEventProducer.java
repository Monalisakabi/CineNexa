package com.smartticket.backend.kafka.producer;

import com.smartticket.backend.kafka.dto.BookingCreatedEvent;
import com.smartticket.backend.kafka.dto.BookingStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingEventProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingEventProducer.class);

    private final KafkaTemplate<String, BookingCreatedEvent> bookingKafkaTemplate;
    private final KafkaTemplate<String, BookingStatusEvent> statusKafkaTemplate;

    public BookingEventProducer(
            KafkaTemplate<String, BookingCreatedEvent> bookingKafkaTemplate,
            KafkaTemplate<String, BookingStatusEvent> statusKafkaTemplate) {
        this.bookingKafkaTemplate = bookingKafkaTemplate;
        this.statusKafkaTemplate = statusKafkaTemplate;
    }

    public void publishBookingCreatedEvent(BookingCreatedEvent event) {
        bookingKafkaTemplate.send(
                "booking.created",
                String.valueOf(event.getBookingId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                logger.error("Failed to publish booking-created event", exception);
            } else {
                logger.info(
                        "Published booking-created event. Topic: {}, Booking ID: {}",
                        result.getRecordMetadata().topic(),
                        event.getBookingId()
                );
            }
        });
    }

    public void publishBookingCancelledEvent(BookingStatusEvent event) {
        publishStatusEvent("booking.cancelled", event);
    }

    public void publishPaymentCompletedEvent(BookingStatusEvent event) {
        publishStatusEvent("payment.completed", event);
    }

    public void publishPaymentFailedEvent(BookingStatusEvent event) {
        publishStatusEvent("payment.failed", event);
    }

    private void publishStatusEvent(
            String topic,
            BookingStatusEvent event) {

        statusKafkaTemplate.send(
                topic,
                String.valueOf(event.getBookingId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                logger.error(
                        "Failed to publish event to topic {} for booking {}",
                        topic,
                        event.getBookingId(),
                        exception
                );
            } else {
                logger.info(
                        "Published event to topic {} for booking {}",
                        result.getRecordMetadata().topic(),
                        event.getBookingId()
                );
            }
        });
    }
}