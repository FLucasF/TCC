package com.pousadamarealta;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationTest {

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        reservationService = new ReservationService(roomService, messageService);
    }

    @Test
    void testStandardReservationHighSeason() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD,
            null
        );

        assertThat(reservation.getSubtotal()).isEqualTo(new BigDecimal("599.76"));
        assertThat(reservation.getDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(reservation.getTotal()).isEqualTo(new BigDecimal("599.76"));
    }

    @Test
    void testCorporateTecnosul() {
        Reservation reservation = reservationService.createReservation(
            "201",
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.CORPORATE,
            "TECNOSUL"
        );

        assertThat(reservation.getSubtotal()).isEqualTo(new BigDecimal("779.80"));
        assertThat(reservation.getDiscount()).isEqualTo(new BigDecimal("93.58"));
        assertThat(reservation.getTotal()).isEqualTo(new BigDecimal("686.22"));
    }

    @Test
    void testWeekendPackageDeluxe() {
        Reservation reservation = reservationService.createReservation(
            "201",
            "Weekend Guest",
            "weekend@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            2,
            RatePlan.WEEKEND_PACKAGE,
            null
        );

        assertThat(reservation.getSubtotal()).isEqualTo(new BigDecimal("820.00"));
        assertThat(reservation.getDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(reservation.getTotal()).isEqualTo(new BigDecimal("820.00"));
    }

    @Test
    void testWeekendPackageCancellationRefund() {
        Reservation reservation = reservationService.createReservation(
            "201",
            "Weekend Guest",
            "weekend@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            2,
            RatePlan.WEEKEND_PACKAGE,
            null
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getRefund()).isEqualTo(new BigDecimal("410.00"));
    }

    @Test
    void testLongStay() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "Long Stay Guest",
            "longstay@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8),
            2,
            RatePlan.LONG_STAY,
            null
        );

        assertThat(reservation.getSubtotal()).isEqualTo(new BigDecimal("1749.30"));
        assertThat(reservation.getDiscount()).isEqualTo(new BigDecimal("174.93"));
        assertThat(reservation.getTotal()).isEqualTo(new BigDecimal("1574.37"));
    }

    @Test
    void testNonRefundable() {
        Reservation reservation = reservationService.createReservation(
            "101",
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.NON_REFUNDABLE,
            null
        );

        BigDecimal expectedSubtotal = new BigDecimal("599.76");
        BigDecimal expectedDiscount = new BigDecimal("89.96");
        BigDecimal expectedTotal = new BigDecimal("509.80");

        assertThat(reservation.getSubtotal()).isEqualTo(expectedSubtotal);
        assertThat(reservation.getDiscount()).isEqualTo(expectedDiscount);
        assertThat(reservation.getTotal()).isEqualTo(expectedTotal);

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertThat(cancelled.getRefund()).isEqualTo(BigDecimal.ZERO);
    }
}
