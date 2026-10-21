package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReservationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppIT {

    @Test
    void shouldCreateAndConfirmReservation() {
        var roomRepo = new RoomRepository();
        var reservationRepo = new ReservationRepository();
        var pricingService = new PricingService();
        var reservationService = new ReservationService(
                reservationRepo, roomRepo, pricingService, null);

        var reservation = reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD);

        assertThat(reservation.getId()).isEqualTo(1);
        assertThat(reservation.getStatus().name()).isEqualTo("PENDING");
    }

    @Test
    void shouldReturnErrorWhenRoomNotFound() {
        var roomRepo = new RoomRepository();
        var reservationRepo = new ReservationRepository();
        var pricingService = new PricingService();
        var reservationService = new ReservationService(
                reservationRepo, roomRepo, pricingService, null);

        assertThatThrownBy(() -> reservationService.createReservation(
                "999", "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD))
                .isInstanceOf(ReservationService.RoomNotFoundException.class);
    }

    @Test
    void shouldReturnErrorWhenCapacityExceeded() {
        var roomRepo = new RoomRepository();
        var reservationRepo = new ReservationRepository();
        var pricingService = new PricingService();
        var reservationService = new ReservationService(
                reservationRepo, roomRepo, pricingService, null);

        assertThatThrownBy(() -> reservationService.createReservation(
                "101", "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                5, RatePlan.STANDARD))
                .isInstanceOf(ReservationService.CapacityExceededException.class);
    }
}
