package br.com.marealta;

import br.com.marealta.domain.*;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    private static final Room STANDARD = RoomCatalog.find(101).orElseThrow();
    private static final Room DELUXE = RoomCatalog.find(201).orElseThrow();

    private static NewReservation request(Room room, String in, String out, RatePlan plan) {
        return new NewReservation(room.number(), "Carla Dias", "carla@example.com", "11122233344",
                LocalDate.parse(in), LocalDate.parse(out), 2, plan);
    }

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Pricing.Quote quote = Pricing.quote(STANDARD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), quote.total());
        assertEquals(2, quote.nights());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2028-03-01")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-06-30")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableGivesFifteenPercentOffRoundedHalfEven() {
        Pricing.Quote quote = Pricing.quote(DELUXE, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), quote.subtotal());
        assertEquals(new BigDecimal("116.97"), quote.discount());
        assertEquals(new BigDecimal("662.83"), quote.total());
        assertEquals(new BigDecimal("0.12"), Money.round(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Money.round(new BigDecimal("0.135")));
    }

    @Test
    void receiptText() {
        Reservation r = Reservation.pending(DELUXE, request(DELUXE, "2027-03-10", "2027-03-12", RatePlan.STANDARD));
        r.assignId(7);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, DELUXE).toText());
    }

    @Test
    void receiptWithDiscountAndSingleNight() {
        Reservation r = Reservation.pending(DELUXE, request(DELUXE, "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE));
        r.assignId(1);
        String text = Receipt.of(r, DELUXE).toText();
        assertTrue(text.contains("| 1 diária\n"));
        assertTrue(text.contains("Desconto: R$ 58,48\nTotal: R$ 331,42"));
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
    }

    @Test
    void lifecycleAndRefunds() {
        Reservation r = Reservation.pending(STANDARD, request(STANDARD, "2027-03-10", "2027-03-12", RatePlan.STANDARD));
        assertThrows(ApiException.class, r::registerCheckIn);
        r.confirm();
        assertEquals(r.total(), r.amountPaid());
        assertThrows(ApiException.class, r::confirm);
        r.cancel();
        assertEquals(r.total(), r.refund());
        assertEquals(Money.ZERO, r.amountKept());
        assertThrows(ApiException.class, r::cancel);

        Reservation nr = Reservation.pending(STANDARD, request(STANDARD, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE));
        nr.confirm();
        nr.cancel();
        assertEquals(Money.ZERO, nr.refund());
        assertEquals(nr.total(), nr.amountKept());

        Reservation pending = Reservation.pending(STANDARD, request(STANDARD, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE));
        pending.cancel();
        assertEquals(Money.ZERO, pending.refund());
    }

    @Test
    void cannotCancelAfterCheckIn() {
        Reservation r = Reservation.pending(STANDARD, request(STANDARD, "2027-03-10", "2027-03-12", RatePlan.STANDARD));
        r.confirm();
        r.registerCheckIn();
        assertThrows(ApiException.class, r::cancel);
        r.registerCheckOut();
        assertThrows(ApiException.class, r::registerCheckOut);
    }

    @Test
    void invalidRequestsAreRejected() {
        assertThrows(ApiException.class, () -> request(STANDARD, "2027-03-10", "2027-03-10", RatePlan.STANDARD));
        assertThrows(ApiException.class, () -> new NewReservation(101, "a", "b", "c",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 0, RatePlan.STANDARD));
    }

    private static Reservation paid(long id, Room room, String in, String out, RatePlan plan) {
        Reservation r = Reservation.pending(room, request(room, in, out, plan));
        r.assignId(id);
        r.confirm();
        return r;
    }

    @Test
    void reports() {
        Reservation a = paid(2, DELUXE, "2027-07-10", "2027-07-12", RatePlan.STANDARD);
        Reservation b = paid(1, STANDARD, "2027-07-09", "2027-07-11", RatePlan.STANDARD);
        Reservation pending = Reservation.pending(STANDARD, request(STANDARD, "2027-07-10", "2027-07-11", RatePlan.STANDARD));
        pending.assignId(3);
        assertEquals("""
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-09,2027-07-11
                201,DELUXE,2,2027-07-10,2027-07-12
                total,2/5
                """, OccupancyReport.render(LocalDate.parse("2027-07-10"), List.of(a, b, pending)));

        Reservation cancelled = paid(4, STANDARD, "2027-07-01", "2027-07-02", RatePlan.NON_REFUNDABLE);
        cancelled.cancel();
        Reservation refunded = paid(5, STANDARD, "2027-07-01", "2027-07-02", RatePlan.STANDARD);
        refunded.cancel();
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                4,101,2027-07-01,2027-07-02,CANCELLED,254.90
                1,101,2027-07-09,2027-07-11,CONFIRMED,599.76
                2,201,2027-07-10,2027-07-12,CONFIRMED,935.76
                total,1790.42
                """, RevenueReport.render(List.of(a, b, pending, cancelled, refunded)));
        assertEquals("reservation,room,check_in,check_out,status,amount\ntotal,0.00\n", RevenueReport.render(List.of()));
    }
}
