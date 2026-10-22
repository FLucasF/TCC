package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Guest;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static final Room STANDARD = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
    private static final Room DELUXE = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
    private static final Guest CARLA = new Guest("Carla Dias", "carla@example.com", "111.222.333-44");

    private static Reservation reserve(Room room, String in, String out, RatePlan plan) {
        return Reservation.pending(room, CARLA, LocalDate.parse(in), LocalDate.parse(out), 2, plan);
    }

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Reservation r = reserve(STANDARD, "2027-07-10", "2027-07-12", RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), r.quote().total());
        assertEquals(2, r.quote().nights());
    }

    @Test
    void seasonBoundariesArePricedNightByNight() {
        // 14/12 normal, 15/12 alta
        Reservation r = reserve(STANDARD, "2027-12-14", "2027-12-16", RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), r.quote().subtotal());
        // 28/02 alta, 01/03 normal
        Reservation m = reserve(STANDARD, "2027-02-28", "2027-03-02", RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), m.quote().subtotal());
    }

    @Test
    void nonRefundableGivesFifteenPercentOffTheSubtotal() {
        Reservation r = reserve(DELUXE, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), r.quote().subtotal());
        assertEquals(new BigDecimal("116.97"), r.quote().discount());
        assertEquals(new BigDecimal("662.83"), r.quote().total());
    }

    @Test
    void roundingIsHalfEven() {
        assertEquals(new BigDecimal("0.12"), Money.round(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Money.round(new BigDecimal("0.135")));
    }

    @Test
    void receiptTextMatchesTheAgreedLayout() {
        Reservation r = new Reservation(7L, 201, CARLA, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                2, RatePlan.STANDARD, reserve(DELUXE, "2027-03-10", "2027-03-12", RatePlan.STANDARD).quote(),
                br.com.marealta.domain.ReservationStatus.CONFIRMED, new BigDecimal("779.80"), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, DELUXE).text());
    }

    @Test
    void receiptShowsDiscountAndSingularNightAndThousands() {
        Room suite = new Room(301, "SUITE", 4, new BigDecimal("579.90"));
        Reservation r = new Reservation(8L, 301, CARLA, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-13"),
                2, RatePlan.NON_REFUNDABLE, reserve(suite, "2027-03-10", "2027-03-13", RatePlan.NON_REFUNDABLE).quote(),
                br.com.marealta.domain.ReservationStatus.CONFIRMED, new BigDecimal("1478.75"), null);
        String text = Receipt.of(r, suite).text();
        assertTrue(text.contains("3 diárias"));
        assertTrue(text.contains("Diárias: R$ 1.739,70\nDesconto: R$ 260,96\nTotal: R$ 1.478,74"), text);
        Reservation one = reserve(suite, "2027-03-10", "2027-03-11", RatePlan.STANDARD);
        assertTrue(Receipt.of(new Reservation(1L, 301, CARLA, one.arrival(), one.departure(), 2, RatePlan.STANDARD,
                one.quote(), one.status(), one.amountPaid(), null), suite).text().contains("| 1 diária\n"));
    }

    @Test
    void lifecycleAndRefunds() {
        Reservation r = reserve(DELUXE, "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        assertThrows(DomainException.class, r::checkIn);
        r.confirm();
        assertEquals(new BigDecimal("779.80"), r.amountPaid());
        assertThrows(DomainException.class, r::confirm);
        r.cancel();
        assertEquals(new BigDecimal("779.80"), r.refund());
        assertEquals(Money.ZERO, r.kept());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, assertThrows(DomainException.class, r::cancel).code());
    }

    @Test
    void cancellationRefunds() {
        Reservation pending = reserve(DELUXE, "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        assertNull(pending.refund());
        pending.cancel();
        assertEquals(Money.ZERO, pending.refund());

        Reservation nonRefundable = reserve(DELUXE, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE);
        nonRefundable.confirm();
        nonRefundable.cancel();
        assertEquals(Money.ZERO, nonRefundable.refund());
        assertEquals(new BigDecimal("662.83"), nonRefundable.kept());

        Reservation checkedIn = reserve(DELUXE, "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        checkedIn.confirm();
        checkedIn.checkIn();
        assertThrows(DomainException.class, checkedIn::cancel);
        checkedIn.checkOut();
        assertFalse(checkedIn.status().canFollow(checkedIn.status()));
    }
}
