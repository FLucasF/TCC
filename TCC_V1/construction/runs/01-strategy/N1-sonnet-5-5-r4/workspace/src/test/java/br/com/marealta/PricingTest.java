package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.marealta.plan.Quote;
import br.com.marealta.plan.RatePlans;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {

    private static Quote quote(String plan, int room, String in, String out, String company) {
        Stay stay = new Stay(Rooms.find(room).orElseThrow(), LocalDate.parse(in), LocalDate.parse(out), company);
        return RatePlans.find(plan).orElseThrow().quote(stay);
    }

    @Test
    void highSeasonNights() {
        Quote q = quote("STANDARD", 101, "2027-07-10", "2027-07-12", null);
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = quote("NON_REFUNDABLE", 101, "2027-03-01", "2027-03-03", null);
        assertEquals(Money.of("499.80"), q.subtotal());
        assertEquals(Money.of("74.97"), q.discount());
    }

    @Test
    void corporate() {
        Quote q = quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("93.58"), q.discount());
        assertEquals(Money.of("686.22"), q.total());
    }

    @Test
    void corporateNeedsKnownCompany() {
        assertThrows(ApiError.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", null));
        assertThrows(ApiError.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test
    void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-14", null);
        assertEquals(Money.of("820.00"), q.total());
        assertEquals(Money.of("410.00"), RatePlans.find("WEEKEND_PACKAGE").orElseThrow().refund(q.total()));
        assertEquals(Money.of("1200.00"), quote("WEEKEND_PACKAGE", 301, "2027-07-09", "2027-07-11", null).total());
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        assertThrows(ApiError.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-11", "2027-03-13", null));
        assertThrows(ApiError.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void longStay() {
        Quote q = quote("LONG_STAY", 101, "2027-03-01", "2027-03-08", null);
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(ApiError.class, () -> quote("LONG_STAY", 101, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void receiptText() {
        Reservation r = new Reservation(7, 201, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, "STANDARD",
                Money.of("779.80"), Money.ZERO, Money.of("779.80"), ReservationStatus.CONFIRMED,
                Money.of("779.80"), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r).text());
    }
}
