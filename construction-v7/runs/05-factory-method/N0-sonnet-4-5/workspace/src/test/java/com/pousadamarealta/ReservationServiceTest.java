package com.pousadamarealta;

import com.pousadamarealta.model.*;
import com.pousadamarealta.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ReservationServiceTest {

    private ReservationService reservationService;
    private RoomService roomService;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        messageService = new MessageService();
        reservationService = new ReservationService(roomService, messageService);
    }

    @Test
    void shouldCreateReservation() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");

        Reservation reservation = reservationService.createReservation(
            "201",
            "Carla Dias",
            "carla@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertThat(reservation.getId()).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualByComparingTo("779.80");
        assertThat(reservation.getDiscount()).isEqualByComparingTo("0.00");
        assertThat(reservation.getTotal()).isEqualByComparingTo("779.80");
    }

    @Test
    void shouldThrowWhenRoomNotFound() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");

        assertThatThrownBy(() -> reservationService.createReservation(
            "999",
            "Test",
            "test@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        ))
        .isInstanceOf(ServiceException.class)
        .hasFieldOrPropertyWithValue("errorCode", "ROOM_NOT_FOUND");
    }

    @Test
    void shouldThrowWhenCapacityExceeded() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");

        assertThatThrownBy(() -> reservationService.createReservation(
            "101",
            "Test",
            "test@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            5,
            RatePlan.STANDARD
        ))
        .isInstanceOf(ServiceException.class)
        .hasFieldOrPropertyWithValue("errorCode", "CAPACITY_EXCEEDED");
    }

    @Test
    void shouldThrowWhenRoomUnavailable() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");

        reservationService.createReservation(
            "201",
            "First Guest",
            "first@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 15),
            2,
            RatePlan.STANDARD
        );

        assertThatThrownBy(() -> reservationService.createReservation(
            "201",
            "Second Guest",
            "second@example.com",
            id,
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            2,
            RatePlan.STANDARD
        ))
        .isInstanceOf(ServiceException.class)
        .hasFieldOrPropertyWithValue("errorCode", "ROOM_UNAVAILABLE");
    }

    @Test
    void shouldAllowOverlappingReservationsWhenOneIsCancelled() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");

        Reservation first = reservationService.createReservation(
            "201",
            "First Guest",
            "first@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 15),
            2,
            RatePlan.STANDARD
        );

        reservationService.cancelReservation(first.getId());

        assertThatCode(() -> reservationService.createReservation(
            "201",
            "Second Guest",
            "second@example.com",
            id,
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            2,
            RatePlan.STANDARD
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldConfirmReservation() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");
        Reservation reservation = reservationService.createReservation(
            "201",
            "Carla Dias",
            "carla@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());

        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(confirmed.getAmountPaid()).isEqualByComparingTo(confirmed.getTotal());
        assertThat(messageService.getAllMessages()).hasSize(1);
    }

    @Test
    void shouldCancelPendingReservation() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");
        Reservation reservation = reservationService.createReservation(
            "201",
            "Test",
            "test@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getRefund()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRefundStandardReservation() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");
        Reservation reservation = reservationService.createReservation(
            "201",
            "Test",
            "test@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getRefund()).isEqualByComparingTo(cancelled.getAmountPaid());
    }

    @Test
    void shouldNotRefundNonRefundableReservation() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");
        Reservation reservation = reservationService.createReservation(
            "201",
            "Test",
            "test@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getRefund()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCheckIn() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");
        Reservation reservation = reservationService.createReservation(
            "201",
            "Test",
            "test@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation checkedIn = reservationService.checkIn(reservation.getId());

        assertThat(checkedIn.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
    }

    @Test
    void shouldCheckOut() {
        GuestIdentification id = GuestIdentification.withCpf("111.222.333-44");
        Reservation reservation = reservationService.createReservation(
            "201",
            "Test",
            "test@example.com",
            id,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        reservationService.checkIn(reservation.getId());
        Document document = reservationService.checkOut(reservation.getId());

        assertThat(document.getReservationId()).isEqualTo(reservation.getId());
        assertThat(document.getText()).contains("Comprovante de reserva");
    }
}
