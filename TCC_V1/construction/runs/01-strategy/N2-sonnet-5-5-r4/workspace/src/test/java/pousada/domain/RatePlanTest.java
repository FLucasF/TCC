package pousada.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, Money.of("249.90"));
    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, Money.of("389.90"));
    private final CorporatePlan corporate = new CorporatePlan(Map.of("TECNOSUL", Money.percent(12)));
    private final WeekendPackagePlan weekend = new WeekendPackagePlan(Map.of(RoomType.DELUXE, Money.of("820.00")));

    private Booking booking(Room room, String in, String out, String company) {
        return new Booking(room, LocalDate.parse(in), LocalDate.parse(out), 2, company);
    }

    @Test
    void highSeasonNights() {
        Quote q = new StandardPlan().quote(booking(standard, "2027-07-10", "2027-07-12", null));
        assertEquals(Money.of("599.76"), q.subtotal());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundableGivesNothingBack() {
        assertEquals(Money.ZERO, new NonRefundablePlan().refund(Money.of("100")));
    }

    @Test
    void corporate() {
        Quote q = corporate.quote(booking(deluxe, "2027-03-10", "2027-03-12", "TECNOSUL"));
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("93.58"), q.discount());
        assertEquals(Money.of("686.22"), q.total());
        assertThrows(ApiException.class, () -> corporate.validate(booking(deluxe, "2027-03-10", "2027-03-12", null)));
        assertThrows(ApiException.class, () -> corporate.validate(booking(deluxe, "2027-03-10", "2027-03-12", "X")));
    }

    @Test
    void weekendPackage() {
        Booking ok = booking(deluxe, "2027-03-12", "2027-03-14", null);
        weekend.validate(ok);
        assertEquals(Money.of("820.00"), weekend.quote(ok).total());
        assertEquals(Money.of("410.00"), weekend.refund(Money.of("820.00")));
        assertThrows(ApiException.class, () -> weekend.validate(booking(deluxe, "2027-03-13", "2027-03-15", null)));
        assertThrows(ApiException.class, () -> weekend.validate(booking(deluxe, "2027-03-12", "2027-03-15", null)));
    }

    @Test
    void longStay() {
        LongStayPlan plan = new LongStayPlan();
        Booking b = booking(standard, "2027-03-01", "2027-03-08", null);
        plan.validate(b);
        Quote q = plan.quote(b);
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(ApiException.class, () -> plan.validate(booking(standard, "2027-03-01", "2027-03-07", null)));
    }

    @Test
    void receiptText() {
        Reservation r = new Reservation(7, 201, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, "STANDARD", 2,
                Money.of("779.80"), Money.ZERO, Money.of("779.80"), ReservationStatus.CONFIRMED,
                Money.of("779.80"), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, deluxe).toText());
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
    }
}
