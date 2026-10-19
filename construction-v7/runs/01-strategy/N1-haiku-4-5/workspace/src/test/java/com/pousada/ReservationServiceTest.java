package com.pousada;

import com.pousada.models.*;
import com.pousada.services.ReservationService;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class ReservationServiceTest {
    private final ReservationService service = new ReservationService();

    @Test
    public void testStandardReservation() {
        String checkIn = "2027-07-10";
        String checkOut = "2027-07-12";

        Reservation res = service.createReservation(101, "João Silva", "joao@example.com", "12345678901",
                                                   checkIn, checkOut, 1, "STANDARD", null);

        assertEquals(2, res.getNights());
        assertEquals(0, res.getDiscount().compareTo(BigDecimal.ZERO));

        BigDecimal expectedSubtotal = BigDecimal.valueOf(599.76);
        assertEquals(0, res.getSubtotal().compareTo(expectedSubtotal));
        assertEquals(0, res.getTotal().compareTo(expectedSubtotal));
    }

    @Test
    public void testNonRefundableReservation() {
        String checkIn = "2027-03-10";
        String checkOut = "2027-03-12";

        Reservation res = service.createReservation(201, "Maria Santos", "maria@example.com", "12345678901",
                                                   checkIn, checkOut, 1, "NON_REFUNDABLE", null);

        assertEquals(2, res.getNights());
        BigDecimal expectedSubtotal = BigDecimal.valueOf(779.80);
        BigDecimal expectedDiscount = BigDecimal.valueOf(116.97);
        BigDecimal expectedTotal = BigDecimal.valueOf(662.83);

        assertEquals(0, res.getSubtotal().compareTo(expectedSubtotal));
    }

    @Test
    public void testCorporateReservation() {
        String checkIn = "2027-03-10";
        String checkOut = "2027-03-12";

        Reservation res = service.createReservation(201, "Carlos", "carlos@example.com", "12345678901",
                                                   checkIn, checkOut, 1, "CORPORATE", "TECNOSUL");

        BigDecimal expectedSubtotal = BigDecimal.valueOf(779.80);
        BigDecimal expectedDiscount = BigDecimal.valueOf(93.58);
        BigDecimal expectedTotal = BigDecimal.valueOf(686.22);

        assertEquals(0, res.getSubtotal().compareTo(expectedSubtotal));
        assertEquals(0, res.getDiscount().compareTo(expectedDiscount));
        assertEquals(0, res.getTotal().compareTo(expectedTotal));
    }

    @Test
    public void testWeekendPackageReservation() {
        String checkIn = "2027-03-12";
        String checkOut = "2027-03-14";

        Reservation res = service.createReservation(201, "Ana", "ana@example.com", "12345678901",
                                                   checkIn, checkOut, 1, "WEEKEND_PACKAGE", null);

        assertEquals(2, res.getNights());
        BigDecimal expectedTotal = BigDecimal.valueOf(820.00);
        assertEquals(0, res.getTotal().compareTo(expectedTotal));
        assertEquals(0, res.getDiscount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testLongStayReservation() {
        String checkIn = "2027-03-01";
        String checkOut = "2027-03-08";

        Reservation res = service.createReservation(101, "Pedro", "pedro@example.com", "12345678901",
                                                   checkIn, checkOut, 1, "LONG_STAY", null);

        assertEquals(7, res.getNights());
        BigDecimal expectedSubtotal = BigDecimal.valueOf(1749.30);
        BigDecimal expectedDiscount = BigDecimal.valueOf(174.93);
        BigDecimal expectedTotal = BigDecimal.valueOf(1574.37);

        assertEquals(0, res.getSubtotal().compareTo(expectedSubtotal));
        assertEquals(0, res.getDiscount().compareTo(expectedDiscount));
        assertEquals(0, res.getTotal().compareTo(expectedTotal));
    }

    @Test
    public void testInvalidCompanyCode() {
        assertThrows(ReservationService.ApiException.class, () -> {
            service.createReservation(201, "Test", "test@example.com", "12345678901",
                                    "2027-03-10", "2027-03-12", 1, "CORPORATE", "INVALID");
        });
    }

    @Test
    public void testMissingCompanyCode() {
        assertThrows(ReservationService.ApiException.class, () -> {
            service.createReservation(201, "Test", "test@example.com", "12345678901",
                                    "2027-03-10", "2027-03-12", 1, "CORPORATE", null);
        });
    }

    @Test
    public void testCapacityExceeded() {
        assertThrows(ReservationService.ApiException.class, () -> {
            service.createReservation(101, "Test", "test@example.com", "12345678901",
                                    "2027-03-10", "2027-03-12", 3, "STANDARD", null);
        });
    }

    @Test
    public void testRoomUnavailable() {
        service.createReservation(101, "Guest 1", "guest1@example.com", "12345678901",
                                "2027-03-10", "2027-03-15", 1, "STANDARD", null);

        assertThrows(ReservationService.ApiException.class, () -> {
            service.createReservation(101, "Guest 2", "guest2@example.com", "12345678901",
                                    "2027-03-12", "2027-03-17", 1, "STANDARD", null);
        });
    }

    @Test
    public void testInvalidCheckOutDate() {
        assertThrows(ReservationService.ApiException.class, () -> {
            service.createReservation(101, "Test", "test@example.com", "12345678901",
                                    "2027-03-15", "2027-03-12", 1, "STANDARD", null);
        });
    }

    @Test
    public void testZeroGuests() {
        assertThrows(ReservationService.ApiException.class, () -> {
            service.createReservation(101, "Test", "test@example.com", "12345678901",
                                    "2027-03-10", "2027-03-12", 0, "STANDARD", null);
        });
    }

    @Test
    public void testReservationStatusFlow() {
        Reservation res = service.createReservation(101, "Test", "test@example.com", "12345678901",
                                                   "2027-03-10", "2027-03-12", 1, "STANDARD", null);

        assertEquals(Reservation.ReservationStatus.PENDING, res.getStatus());

        Reservation confirmed = service.confirmReservation(res.getId(), res.getTotal());
        assertEquals(Reservation.ReservationStatus.CONFIRMED, confirmed.getStatus());

        Reservation checkedIn = service.checkIn(res.getId());
        assertEquals(Reservation.ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Reservation checkedOut = service.checkOut(res.getId());
        assertEquals(Reservation.ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testCancellationRefund() {
        Reservation res = service.createReservation(101, "Test", "test@example.com", "12345678901",
                                                   "2027-03-10", "2027-03-12", 1, "STANDARD", null);

        service.confirmReservation(res.getId(), res.getTotal());
        Reservation cancelled = service.cancelReservation(res.getId());

        assertEquals(Reservation.ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, cancelled.getRefund().compareTo(res.getTotal()));
    }

    @Test
    public void testNonRefundableCancellation() {
        Reservation res = service.createReservation(101, "Test", "test@example.com", "12345678901",
                                                   "2027-03-10", "2027-03-12", 1, "NON_REFUNDABLE", null);

        service.confirmReservation(res.getId(), res.getTotal());
        Reservation cancelled = service.cancelReservation(res.getId());

        assertEquals(Reservation.ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, cancelled.getRefund().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testWeekendPackageCancellationRefund() {
        Reservation res = service.createReservation(201, "Test", "test@example.com", "12345678901",
                                                   "2027-03-12", "2027-03-14", 1, "WEEKEND_PACKAGE", null);

        service.confirmReservation(res.getId(), res.getTotal());
        Reservation cancelled = service.cancelReservation(res.getId());

        BigDecimal expectedRefund = BigDecimal.valueOf(410.00);
        assertEquals(0, cancelled.getRefund().compareTo(expectedRefund));
    }
}
