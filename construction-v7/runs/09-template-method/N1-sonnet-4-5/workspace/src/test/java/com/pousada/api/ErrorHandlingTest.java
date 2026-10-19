package com.pousada.api;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.service.*;
import com.pousada.domain.validation.ValidationException;
import com.pousada.infrastructure.repository.MessageRepository;
import com.pousada.infrastructure.repository.ReservationRepository;
import com.pousada.infrastructure.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ErrorHandlingTest {
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        RoomService roomService = new RoomService(roomRepository);
        reservationService = new ReservationService(reservationRepository, roomService);
    }

    @Test
    void shouldRejectNonExistentRoom() {
        ValidationException exception = catchThrowableOfType(() ->
            reservationService.createReservation(
                "999", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
            ), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("ROOM_NOT_FOUND");
    }

    @Test
    void shouldRejectCheckOutBeforeCheckIn() {
        ValidationException exception = catchThrowableOfType(() ->
            reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 10),
                2, RatePlan.STANDARD
            ), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldRejectZeroGuests() {
        ValidationException exception = catchThrowableOfType(() ->
            reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                0, RatePlan.STANDARD
            ), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldRejectExcessGuests() {
        ValidationException exception = catchThrowableOfType(() ->
            reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                5, RatePlan.STANDARD
            ), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("CAPACITY_EXCEEDED");
    }

    @Test
    void shouldRejectNonExistentReservation() {
        ValidationException exception = catchThrowableOfType(() ->
            reservationService.getReservation(999L), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("RESERVATION_NOT_FOUND");
    }

    @Test
    void shouldRejectConfirmingAlreadyConfirmedReservation() {
        var reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());

        ValidationException exception = catchThrowableOfType(() ->
            reservationService.confirmReservation(reservation.getId()), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("OPERATION_NOT_ALLOWED");
    }

    @Test
    void shouldRejectCheckInOfPendingReservation() {
        var reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        ValidationException exception = catchThrowableOfType(() ->
            reservationService.checkInReservation(reservation.getId()), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("OPERATION_NOT_ALLOWED");
    }

    @Test
    void shouldRejectCancellingCheckedInReservation() {
        var reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678900",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(reservation.getId());
        reservationService.checkInReservation(reservation.getId());

        ValidationException exception = catchThrowableOfType(() ->
            reservationService.cancelReservation(reservation.getId()), ValidationException.class);

        assertThat(exception.getErrorCode()).isEqualTo("OPERATION_NOT_ALLOWED");
    }
}
