package br.com.marealta.pousada;

import br.com.marealta.pousada.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    private static final Room STANDARD = new Room(101, RoomType.STANDARD);
    private static final Room DELUXE = new Room(201, RoomType.DELUXE);

    private static Reservation reserve(Room room, String in, String out, int guests, RatePlan plan) {
        return Reservation.create(room, "Carla Dias", "carla@example.com", "111.222.333-44",
                LocalDate.parse(in), LocalDate.parse(out), guests, plan);
    }

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Reservation r = reserve(STANDARD, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), r.total());
        assertEquals(2, r.nights());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2028-03-01")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-06-30")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-07-31")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableDiscountsFifteenPercentHalfEven() {
        Reservation r = reserve(STANDARD, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("249.90"), r.subtotal());
        assertEquals(new BigDecimal("37.48"), r.discount()); // 37.485 -> par
        assertEquals(new BigDecimal("212.42"), r.total());
    }

    @Test
    void moneyAndDateFormatting() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.brl(BigDecimal.ZERO));
        assertEquals("10/03/2027", Money.brDate(LocalDate.parse("2027-03-10")));
    }

    @Test
    void receiptText() {
        Reservation r = new Reservation(7, DELUXE, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD,
                new BigDecimal("779.80"), Money.ZERO, new BigDecimal("779.80"), ReservationStatus.CONFIRMED,
                new BigDecimal("779.80"), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.receipt().text());
    }

    @Test
    void receiptWithDiscountAndSingleNight() {
        Reservation r = reserve(STANDARD, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE);
        r.assignId(3);
        r.confirm();
        String text = r.receipt().text();
        assertTrue(text.contains("| 1 diária\n"));
        assertTrue(text.contains("Desconto: R$ 37,48\nTotal: R$ 212,42"));
    }

    @Test
    void lifecycleAndRefunds() {
        Reservation pending = reserve(STANDARD, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD);
        assertThrows(PousadaException.class, pending::startStay);
        assertThrows(PousadaException.class, pending::receipt);
        pending.cancel();
        assertEquals(Money.ZERO, pending.refund());

        Reservation paid = reserve(STANDARD, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD);
        paid.confirm();
        assertEquals(paid.total(), paid.amountPaid());
        paid.cancel();
        assertEquals(paid.total(), paid.refund());
        assertEquals(Money.ZERO, paid.amountKept());

        Reservation locked = reserve(STANDARD, "2027-03-10", "2027-03-12", 1, RatePlan.NON_REFUNDABLE);
        locked.confirm();
        locked.cancel();
        assertEquals(Money.ZERO, locked.refund());
        assertEquals(locked.total(), locked.amountKept());

        Reservation stay = reserve(STANDARD, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD);
        stay.confirm();
        stay.startStay();
        assertThrows(PousadaException.class, stay::cancel);
        assertThrows(PousadaException.class, stay::confirm);
        stay.endStay();
        assertThrows(PousadaException.class, stay::endStay);
    }

    @Test
    void capacityAndGuestsValidation() {
        PousadaException e = assertThrows(PousadaException.class,
                () -> reserve(STANDARD, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD));
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, e.code());
        e = assertThrows(PousadaException.class,
                () -> reserve(STANDARD, "2027-03-10", "2027-03-12", 0, RatePlan.STANDARD));
        assertEquals(ErrorCode.INVALID_REQUEST, e.code());
    }
}
