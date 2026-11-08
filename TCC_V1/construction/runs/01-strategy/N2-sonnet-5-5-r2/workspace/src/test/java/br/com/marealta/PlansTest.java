package br.com.marealta;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.domain.Rooms;
import br.com.marealta.domain.Stay;
import br.com.marealta.plan.RatePlans;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlansTest {
    private static Quote quote(String plan, int room, String in, String out, String company) {
        Room r = Rooms.find(room).orElseThrow();
        return RatePlans.byCode(plan).quote(new Stay(r, LocalDate.parse(in), LocalDate.parse(out), company));
    }

    @Test
    void standardInHighSeason() {
        Quote q = quote("STANDARD", 101, "2027-07-10", "2027-07-12", null);
        assertEquals(Money.of("599.76"), q.total());
        assertEquals(2, q.nights());
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
        assertThrows(BusinessException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", null));
        assertThrows(BusinessException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "tecnosul"));
        assertThrows(BusinessException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test
    void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-14", null);
        assertEquals(Money.of("820.00"), q.total());
        assertEquals(Money.ZERO, q.discount());
        assertEquals(Money.of("410.00"), RatePlans.byCode("WEEKEND_PACKAGE").refund(Money.of("820.00")));
        // alta temporada não muda o preço fechado
        assertEquals(Money.of("1200.00"), quote("WEEKEND_PACKAGE", 301, "2027-07-09", "2027-07-11", null).total());
        assertThrows(BusinessException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-13", "2027-03-15", null));
        assertThrows(BusinessException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void longStay() {
        Quote q = quote("LONG_STAY", 101, "2027-03-01", "2027-03-08", null);
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(BusinessException.class, () -> quote("LONG_STAY", 101, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void refunds() {
        assertEquals(Money.of("100.00"), RatePlans.byCode("STANDARD").refund(Money.of("100.00")));
        assertEquals(Money.ZERO, RatePlans.byCode("NON_REFUNDABLE").refund(Money.of("100.00")));
    }

    @Test
    void unknownPlan() {
        assertThrows(BusinessException.class, () -> RatePlans.byCode("VIP"));
    }

    @Test
    void receiptText() {
        Receipt r = new Receipt(7, "Carla Dias", "111.222.333-44", 201, RoomType.DELUXE,
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2,
                Money.of("779.80"), Money.ZERO, Money.of("779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.toText());
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
    }
}
