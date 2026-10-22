package pousada;

import org.junit.jupiter.api.Test;
import pousada.domain.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    private final RoomCatalog rooms = new RoomCatalog();

    private Reservation reservation(int room, String in, String out, int guests, RatePlan plan) {
        return Reservation.open(rooms.find(room).orElseThrow(), new Guest("Carla Dias", "c@x.com", "111.222.333-44"),
                LocalDate.parse(in), LocalDate.parse(out), guests, plan);
    }

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Quote q = reservation(101, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD).quote();
        assertEquals(Money.of("599.76"), q.subtotal());
        assertEquals(Money.of("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-12-15")));
        assertTrue(Season.isHigh(LocalDate.parse("2028-02-29")));
        assertFalse(Season.isHigh(LocalDate.parse("2028-03-01")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-06-30")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-07-31")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableDiscountsFifteenPercentHalfEven() {
        Quote q = reservation(201, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE).quote();
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.of("662.83"), q.total());
    }

    @Test
    void rejectsMoreGuestsThanCapacity() {
        var e = assertThrows(DomainException.class, () -> reservation(101, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD));
        assertEquals(DomainException.Code.CAPACITY_EXCEEDED, e.code());
    }

    @Test
    void lifecycleAndRefunds() {
        Reservation r = reservation(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        assertThrows(DomainException.class, r::startStay);
        r.confirm();
        assertEquals(Money.of("779.80"), r.amountPaid());
        assertThrows(DomainException.class, r::confirm);
        r.startStay();
        assertThrows(DomainException.class, r::cancel);
        r.endStay();
        assertEquals(ReservationStatus.CHECKED_OUT, r.status());
    }

    @Test
    void cancellationRefundDependsOnPlan() {
        Reservation normal = reservation(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        normal.confirm();
        normal.cancel();
        assertEquals(Money.of("779.80"), normal.refund());

        Reservation strict = reservation(201, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE);
        strict.confirm();
        strict.cancel();
        assertEquals(Money.ZERO, strict.refund());
        assertEquals(Money.of("662.83"), strict.retained());

        Reservation pending = reservation(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        pending.cancel();
        assertEquals(Money.ZERO, pending.refund());
    }

    @Test
    void documentText() {
        Reservation r = reservation(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD).withId(7);
        r.confirm();
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Document.of(r).render());
    }

    @Test
    void documentWithDiscountAndOneNight() {
        Reservation r = reservation(301, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE).withId(1);
        r.confirm();
        String text = Document.of(r).render();
        assertTrue(text.contains("| 1 diária\n"));
        assertTrue(text.contains("Diárias: R$ 579,90\nDesconto: R$ 86,98\nTotal: R$ 492,92"));
        assertEquals("R$ 1.234,56", Formats.money(Money.of("1234.56")));
    }

    @Test
    void documentOnlyForPaidReservations() {
        assertThrows(DomainException.class,
                () -> Document.of(reservation(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD)));
    }
}
