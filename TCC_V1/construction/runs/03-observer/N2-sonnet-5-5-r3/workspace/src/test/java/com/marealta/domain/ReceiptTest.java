package com.marealta.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReceiptTest {
    private final RoomCatalog rooms = new RoomCatalog();

    private Reservation reservation(LocalDate in, LocalDate out, RatePlan plan) {
        Reservation r = Reservation.open(rooms.require(201), "Carla Dias", "c@x.com", "111.222.333-44", in, out, 2, plan);
        r.assignId(7);
        return r;
    }

    @Test
    void receiptWithoutDiscount() {
        Reservation r = reservation(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, rooms.require(201)).text());
    }

    @Test
    void receiptWithDiscountAndOneNight() {
        Reservation r = reservation(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        String text = Receipt.of(r, rooms.require(201)).text();
        assertEquals(true, text.contains("| 1 diária\n"));
        assertEquals(true, text.contains("Desconto: R$ 58,48\nTotal: R$ 331,42"));
    }

    @Test
    void thousandsSeparator() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
    }

    @Test
    void cancellationRefundDependsOnPlan() {
        Reservation normal = reservation(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD);
        normal.confirm();
        normal.cancel();
        assertEquals(Money.of("779.80"), normal.refund());

        Reservation nonRefundable = reservation(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        nonRefundable.confirm();
        nonRefundable.cancel();
        assertEquals(Money.of("0"), nonRefundable.refund());
        assertEquals(nonRefundable.amountPaid(), nonRefundable.retained());

        Reservation pending = reservation(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD);
        pending.cancel();
        assertEquals(Money.of("0"), pending.refund());
    }
}
