package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.PousadaException;
import br.com.marealta.pousada.domain.Quote;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DomainTest {

    static final Room R101 = Room.find(101).orElseThrow();
    static final Room R201 = Room.find(201).orElseThrow();

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Quote q = Quote.of(R101, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        // 14/12 é baixa, 15/12 é alta; 28/02 alta, 01/03 baixa
        assertEquals(new BigDecimal("249.90"),
                Quote.of(R101, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 15), RatePlan.STANDARD).total());
        assertEquals(new BigDecimal("299.88"),
                Quote.of(R101, LocalDate.of(2027, 12, 15), LocalDate.of(2027, 12, 16), RatePlan.STANDARD).total());
        assertEquals(new BigDecimal("299.88"),
                Quote.of(R101, LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 1), RatePlan.STANDARD).total());
        assertEquals(new BigDecimal("249.90"),
                Quote.of(R101, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 2), RatePlan.STANDARD).total());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        // 389.90 * 2 = 779.80; 15% = 116.97 exato
        Quote q = Quote.of(R201, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
        assertEquals(new BigDecimal("0.12"), Money.scale(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Money.scale(new BigDecimal("0.135")));
    }

    @Test
    void formatsMoney() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.format(Money.ZERO));
    }

    @Test
    void capacityIsChecked() {
        PousadaException e = assertThrows(PousadaException.class, () -> Reservation.open(R101, "A", "a@a", "1",
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 2), 3, RatePlan.STANDARD));
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, e.code());
    }

    @Test
    void stateMachineAndRefunds() {
        Reservation pending = open(RatePlan.STANDARD);
        assertThrows(PousadaException.class, pending::registerCheckIn);
        assertThrows(PousadaException.class, pending::receipt);
        pending.cancel();
        assertEquals(Money.ZERO, pending.refund());
        assertThrows(PousadaException.class, pending::confirm);

        Reservation standard = open(RatePlan.STANDARD);
        standard.confirm();
        assertThrows(PousadaException.class, standard::confirm);
        standard.cancel();
        assertEquals(standard.quote().total(), standard.refund());

        Reservation nonRefundable = open(RatePlan.NON_REFUNDABLE);
        nonRefundable.confirm();
        nonRefundable.cancel();
        assertEquals(Money.ZERO, nonRefundable.refund());
        assertEquals(nonRefundable.quote().total(), nonRefundable.retainedAmount());

        Reservation stay = open(RatePlan.STANDARD);
        stay.confirm();
        assertThrows(PousadaException.class, stay::registerCheckOut);
        stay.registerCheckIn();
        assertThrows(PousadaException.class, stay::cancel);
        stay.registerCheckOut();
    }

    static Reservation open(RatePlan plan) {
        return Reservation.open(R201, "Carla Dias", "carla@x.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, plan).withId(7);
    }
}
