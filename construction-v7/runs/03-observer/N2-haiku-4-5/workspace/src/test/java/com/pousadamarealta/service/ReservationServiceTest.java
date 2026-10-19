package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {
    private ReservationService service;
    private ReservationRepository reservationRepo;
    private MessageRepository messageRepo;

    @BeforeEach
    public void setup() {
        reservationRepo = new ReservationRepository();
        RoomRepository roomRepo = new RoomRepository();
        messageRepo = new MessageRepository();
        PricingService pricingService = new PricingService();
        service = new ReservationService(reservationRepo, roomRepo, messageRepo, pricingService);
    }

    @Test
    public void testCreateReservationValid() {
        Reservation res = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals(1, res.getId());
        assertEquals("João Silva", res.getGuestName());
        assertEquals(ReservationStatus.PENDING, res.getStatus());
        assertEquals(2, res.getNights());
    }

    @Test
    public void testCreateReservationRoomNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.createReservation(
                999,
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testCreateReservationCapacityExceeded() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.createReservation(
                101,
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                3,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testCreateReservationRoomUnavailable() {
        service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        service.confirmReservation(1);

        assertThrows(IllegalArgumentException.class, () -> {
            service.createReservation(
                101,
                "Maria Santos",
                "maria@example.com",
                "12345678902",
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 3, 13),
                2,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    public void testConfirmReservation() {
        Reservation res = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        Reservation confirmed = service.confirmReservation(res.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
        assertEquals(3, messageRepo.findAll().size());
    }

    @Test
    public void testCheckIn() {
        Reservation res = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        service.confirmReservation(res.getId());

        Reservation checkedIn = service.checkIn(res.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    public void testCheckOut() {
        Reservation res = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        service.confirmReservation(res.getId());
        service.checkIn(res.getId());

        Reservation checkedOut = service.checkOut(res.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testCancelPendingReservation() {
        Reservation res = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        Reservation cancelled = service.cancelReservation(res.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
        assertEquals(0, messageRepo.findAll().size());
    }

    @Test
    public void testCancelConfirmedReservationStandard() {
        Reservation res = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        service.confirmReservation(res.getId());

        Reservation cancelled = service.cancelReservation(res.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getRefund().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testCancelConfirmedReservationNonRefundable() {
        Reservation res = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );
        service.confirmReservation(res.getId());

        Reservation cancelled = service.cancelReservation(res.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    public void testCancelledReservationDoesNotBlockRoom() {
        Reservation res1 = service.createReservation(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );
        service.confirmReservation(res1.getId());
        service.cancelReservation(res1.getId());

        Reservation res2 = service.createReservation(
            101,
            "Maria Santos",
            "maria@example.com",
            "12345678902",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals(2, res2.getId());
    }
}
