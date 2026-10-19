package marealta;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    private final Outbox outbox = new Outbox();
    private final ReservationService service = new ReservationService(outbox);
    private final RatePlans plans = RatePlans.defaults();

    private Reservation book(int room, String in, String out, int guests, String plan, String company) {
        return service.create(new ReservationService.NewReservation(room, "Carla Dias", "c@x.com",
                "111.222.333-44", LocalDate.parse(in), LocalDate.parse(out), guests,
                plans.find(plan).orElseThrow(), company));
    }

    @Test
    void highSeasonStandard() {
        Reservation r = book(101, "2027-07-10", "2027-07-12", 2, "STANDARD", null);
        assertEquals(Money.of("599.76"), r.total());
    }

    @Test
    void corporateTecnosul() {
        Reservation r = book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", "TECNOSUL");
        assertEquals(Money.of("779.80"), r.subtotal());
        assertEquals(Money.of("93.58"), r.discount());
        assertEquals(Money.of("686.22"), r.total());
    }

    @Test
    void corporateNeedsKnownCompany() {
        assertEquals(400, assertThrows(ApiException.class,
                () -> book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", null)).status());
        assertEquals(400, assertThrows(ApiException.class,
                () -> book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", "OUTRA")).status());
    }

    @Test
    void weekendPackageAndRefund() {
        Reservation r = book(201, "2027-03-12", "2027-03-14", 2, "WEEKEND_PACKAGE", null);
        assertEquals(Money.of("820.00"), r.total());
        service.confirm(r.id());
        service.cancel(r.id());
        assertEquals(Money.of("410.00"), r.refund());
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        assertThrows(ApiException.class, () -> book(201, "2027-03-13", "2027-03-15", 2, "WEEKEND_PACKAGE", null));
        assertThrows(ApiException.class, () -> book(201, "2027-03-12", "2027-03-15", 2, "WEEKEND_PACKAGE", null));
    }

    @Test
    void longStay() {
        Reservation r = book(101, "2027-03-01", "2027-03-08", 2, "LONG_STAY", null);
        assertEquals(Money.of("1749.30"), r.subtotal());
        assertEquals(Money.of("174.93"), r.discount());
        assertEquals(Money.of("1574.37"), r.total());
        assertThrows(ApiException.class, () -> book(102, "2027-03-01", "2027-03-07", 2, "LONG_STAY", null));
    }

    @Test
    void nonRefundableCancelKeepsMoney() {
        Reservation r = book(101, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE", null);
        service.confirm(r.id());
        service.cancel(r.id());
        assertEquals(Money.ZERO, r.refund());
        assertEquals(r.total(), r.netAmount());
    }

    @Test
    void overlapAndCapacity() {
        book(101, "2027-03-10", "2027-03-12", 2, "STANDARD", null);
        assertEquals(409, assertThrows(ApiException.class,
                () -> book(101, "2027-03-11", "2027-03-13", 1, "STANDARD", null)).status());
        assertEquals(422, assertThrows(ApiException.class,
                () -> book(102, "2027-03-11", "2027-03-13", 3, "STANDARD", null)).status());
    }

    @Test
    void confirmationSendsEmailWithReceipt() {
        Reservation r = book(201, "2027-03-10", "2027-03-12", 2, "STANDARD", null);
        service.confirm(r.id());
        Message m = outbox.sent().get(0);
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.subject());
        assertTrue(m.body().contains("Diárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada"));
    }
}
