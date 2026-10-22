package com.pousada.maralta.integration;

import com.pousada.maralta.domain.*;
import com.pousada.maralta.repository.*;
import com.pousada.maralta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationFlowTest {
    @Test
    public void testCompleteReservationFlow() {
        PricingService pricingService = new PricingService();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("599.76"), subtotal);
        assertEquals(new BigDecimal("89.96"), discount);
        assertEquals(new BigDecimal("509.80"), total);

        Reservation reservation = new Reservation(
            1L, "101", "João Silva", "joao@example.com",
            new CpfIdentification("111.222.333-44"),
            checkIn, checkOut, 2, RatePlan.NON_REFUNDABLE,
            2, subtotal, discount, total,
            ReservationStatus.PENDING, null, null
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());

        reservation.confirm();
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(total, reservation.getAmountPaid());

        reservation.checkIn();
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    public void testCancellationWithRefund() {
        PricingService pricingService = new PricingService();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(BigDecimal.ZERO, discount);
        assertEquals(new BigDecimal("779.80"), total);

        Reservation reservation = new Reservation(
            1L, "201", "Maria Santos", "maria@example.com",
            new CpfIdentification("222.333.444-55"),
            checkIn, checkOut, 2, RatePlan.STANDARD,
            2, subtotal, discount, total,
            ReservationStatus.PENDING, null, null
        );

        reservation.confirm();
        assertEquals(total, reservation.getAmountPaid());

        reservation.cancel();
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(total, reservation.getRefund());
    }

    @Test
    public void testNonRefundableCancellation() {
        PricingService pricingService = new PricingService();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            1L, "201", "Pedro Costa", "pedro@example.com",
            new CpfIdentification("333.444.555-66"),
            checkIn, checkOut, 2, RatePlan.NON_REFUNDABLE,
            2, subtotal, discount, total,
            ReservationStatus.PENDING, null, null
        );

        reservation.confirm();
        assertEquals(total, reservation.getAmountPaid());

        reservation.cancel();
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());

        assertEquals(total, reservation.getRevenue());
    }
}
