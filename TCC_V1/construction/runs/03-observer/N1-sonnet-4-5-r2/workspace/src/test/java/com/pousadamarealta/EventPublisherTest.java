package com.pousadamarealta;

import com.pousadamarealta.domain.event.*;
import com.pousadamarealta.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EventPublisherTest {

    @Test
    void testPublishAndReceiveEvent() {
        EventPublisher publisher = new EventPublisher();
        List<ReservationConfirmedEvent> receivedEvents = new ArrayList<>();

        publisher.subscribe(ReservationConfirmedEvent.class, receivedEvents::add);

        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservation.setId(1L);

        publisher.publish(new ReservationConfirmedEvent(reservation));

        assertEquals(1, receivedEvents.size());
        assertEquals(reservation, receivedEvents.get(0).reservation());
    }

    @Test
    void testMultipleSubscribers() {
        EventPublisher publisher = new EventPublisher();
        List<String> notifications = new ArrayList<>();

        publisher.subscribe(ReservationConfirmedEvent.class,
            event -> notifications.add("Listener 1"));
        publisher.subscribe(ReservationConfirmedEvent.class,
            event -> notifications.add("Listener 2"));
        publisher.subscribe(ReservationConfirmedEvent.class,
            event -> notifications.add("Listener 3"));

        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservation.setId(1L);

        publisher.publish(new ReservationConfirmedEvent(reservation));

        assertEquals(3, notifications.size());
        assertTrue(notifications.contains("Listener 1"));
        assertTrue(notifications.contains("Listener 2"));
        assertTrue(notifications.contains("Listener 3"));
    }

    @Test
    void testDifferentEventTypes() {
        EventPublisher publisher = new EventPublisher();
        List<String> notifications = new ArrayList<>();

        publisher.subscribe(ReservationConfirmedEvent.class,
            event -> notifications.add("Confirmed"));
        publisher.subscribe(ReservationCancelledEvent.class,
            event -> notifications.add("Cancelled"));

        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservation.setId(1L);

        publisher.publish(new ReservationConfirmedEvent(reservation));
        publisher.publish(new ReservationCancelledEvent(reservation, true));

        assertEquals(2, notifications.size());
        assertEquals("Confirmed", notifications.get(0));
        assertEquals("Cancelled", notifications.get(1));
    }
}
