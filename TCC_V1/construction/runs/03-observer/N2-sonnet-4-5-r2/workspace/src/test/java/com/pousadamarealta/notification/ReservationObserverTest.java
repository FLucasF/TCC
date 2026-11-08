package com.pousadamarealta.notification;

import com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationObserverTest {

    static class TestObserver implements ReservationObserver {
        final List<String> events = new ArrayList<>();

        @Override
        public void onReservationConfirmed(Reservation reservation) {
            events.add("confirmed:" + reservation.getId());
        }

        @Override
        public void onReservationCancelled(Reservation reservation, boolean wasConfirmed) {
            events.add("cancelled:" + reservation.getId() + ":" + wasConfirmed);
        }
    }

    @Test
    void shouldNotifyObserversOnConfirmation() {
        TestObserver observer1 = new TestObserver();
        TestObserver observer2 = new TestObserver();

        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.setId(1L);
        reservation.addObserver(observer1);
        reservation.addObserver(observer2);

        reservation.confirm();

        assertThat(observer1.events).containsExactly("confirmed:1");
        assertThat(observer2.events).containsExactly("confirmed:1");
    }

    @Test
    void shouldNotifyObserversOnCancellationOfConfirmedReservation() {
        TestObserver observer = new TestObserver();

        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.setId(1L);
        reservation.addObserver(observer);

        reservation.confirm();
        reservation.cancel();

        assertThat(observer.events).containsExactly("confirmed:1", "cancelled:1:true");
    }

    @Test
    void shouldNotifyObserversOnCancellationOfPendingReservation() {
        TestObserver observer = new TestObserver();

        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.setId(1L);
        reservation.addObserver(observer);

        reservation.cancel();

        assertThat(observer.events).containsExactly("cancelled:1:false");
    }
}
