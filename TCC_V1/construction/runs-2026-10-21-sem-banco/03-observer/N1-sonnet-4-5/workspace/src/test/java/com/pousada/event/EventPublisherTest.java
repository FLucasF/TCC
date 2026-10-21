package com.pousada.event;

import com.pousada.domain.Money;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventPublisherTest {
    @Test
    void shouldNotifyAllListeners() {
        EventPublisher publisher = new EventPublisher();
        List<String> notifications = new ArrayList<>();

        publisher.register(event -> {
            if (event instanceof ReservationConfirmedEvent) {
                notifications.add("listener1");
            }
        });

        publisher.register(event -> {
            if (event instanceof ReservationConfirmedEvent) {
                notifications.add("listener2");
            }
        });

        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        publisher.publish(new ReservationConfirmedEvent(reservation, new Money(BigDecimal.ZERO)));

        assertEquals(2, notifications.size());
        assertTrue(notifications.contains("listener1"));
        assertTrue(notifications.contains("listener2"));
    }

    @Test
    void shouldAllowSelectiveListening() {
        EventPublisher publisher = new EventPublisher();
        List<String> notifications = new ArrayList<>();

        publisher.register(event -> {
            if (event instanceof ReservationConfirmedEvent) {
                notifications.add("confirmed");
            } else if (event instanceof ReservationCancelledEvent) {
                notifications.add("cancelled");
            }
        });

        Reservation reservation = new Reservation(
            1, "101", "João", "joao@example.com", "12345678900",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        publisher.publish(new ReservationConfirmedEvent(reservation, new Money(BigDecimal.ZERO)));
        publisher.publish(new ReservationCancelledEvent(reservation, new Money(BigDecimal.ZERO), false));

        assertEquals(2, notifications.size());
        assertTrue(notifications.contains("confirmed"));
        assertTrue(notifications.contains("cancelled"));
    }
}
