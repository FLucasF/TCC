package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainTest {

    private static final Room STANDARD = new Room(101, RoomType.STANDARD);
    private static final Room DELUXE = new Room(201, RoomType.DELUXE);

    private static Reservation reservation(Room room, String in, String out, int guests, RatePlan plan) {
        return Reservation.create(room, "Carla Dias", "carla@example.com", "111.222.333-44",
                LocalDate.parse(in), LocalDate.parse(out), guests, plan);
    }

    @Test
    void highSeasonNightsCost20PercentMore() {
        Reservation r = reservation(STANDARD, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD);
        assertEquals(2, r.nights());
        assertEquals(new BigDecimal("599.76"), r.total());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2028-03-01")));
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-06-30")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2027-07-31")));
    }

    @Test
    void nonRefundableGivesFifteenPercentOffUsingBankersRounding() {
        Reservation r = reservation(STANDARD, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("249.90"), r.subtotal());
        assertEquals(new BigDecimal("37.48"), r.discount()); // 37.485 -> par
        assertEquals(new BigDecimal("212.42"), r.total());
    }

    @Test
    void lifecycleFollowsTheOrder() {
        Reservation r = reservation(DELUXE, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        assertEquals(Money.ZERO, r.amountPaid());
        assertThrows(PousadaException.class, r::checkIn);
        r.confirm();
        assertEquals(new BigDecimal("779.80"), r.amountPaid());
        assertThrows(PousadaException.class, r::confirm);
        assertThrows(PousadaException.class, r::checkOut);
        r.checkIn();
        assertThrows(PousadaException.class, r::cancel);
        r.checkOut();
    }

    @Test
    void cancellationRefundsOnlyTheStandardPlanAndOnlyWhatWasPaid() {
        Reservation pending = reservation(DELUXE, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        pending.cancel();
        assertEquals(Money.ZERO, pending.refund());

        Reservation standard = reservation(DELUXE, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        standard.confirm();
        standard.cancel();
        assertEquals(new BigDecimal("779.80"), standard.refund());
        assertEquals(Money.ZERO, standard.retained());

        Reservation nonRefundable = reservation(DELUXE, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE);
        nonRefundable.confirm();
        nonRefundable.cancel();
        assertEquals(Money.ZERO, nonRefundable.refund());
        assertEquals(nonRefundable.total(), nonRefundable.retained());
    }

    @Test
    void refundIsNullUntilCancelled() {
        assertNull(reservation(STANDARD, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD).refund());
    }

    @Test
    void capacityAndGuestsAreChecked() {
        PousadaException tooMany = assertThrows(PousadaException.class,
                () -> reservation(STANDARD, "2027-03-10", "2027-03-11", 3, RatePlan.STANDARD));
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, tooMany.code());
        assertThrows(PousadaException.class,
                () -> reservation(STANDARD, "2027-03-10", "2027-03-11", 0, RatePlan.STANDARD));
    }

    @Test
    void receiptTextMatchesTheModel() {
        Reservation r = reservation(DELUXE, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        r.assignId(7);
        r.confirm();
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.receipt().text());
    }

    @Test
    void receiptShowsDiscountSingularNightAndThousands() {
        Reservation r = Reservation.create(new Room(301, RoomType.SUITE), "Ana", "a@x.com", "1",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.NON_REFUNDABLE);
        r.assignId(1);
        r.confirm();
        String text = r.receipt().text();
        assertEquals(true, text.contains("| 1 diária\n"));
        assertEquals(true, text.contains("Desconto: R$ 86,98\n"));
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
    }

    @Test
    void receiptOnlyForPaidReservations() {
        Reservation r = reservation(DELUXE, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        assertThrows(PousadaException.class, r::receipt);
        r.confirm();
        r.cancel();
        assertThrows(PousadaException.class, r::receipt);
    }
}
