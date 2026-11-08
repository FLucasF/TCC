package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Rooms;
import br.com.marealta.domain.Stay;
import br.com.marealta.plan.RatePlans;

class RatePlanTest {
    private static Stay stay(int room, String in, String out, String company) {
        return new Stay(Rooms.find(room).orElseThrow(), LocalDate.parse(in), LocalDate.parse(out), company);
    }

    private static Quote quote(String plan, Stay stay) {
        RatePlans.find(plan).validate(stay);
        return RatePlans.find(plan).quote(stay);
    }

    @Test
    void highSeasonStandard() {
        Quote q = quote("STANDARD", stay(101, "2027-07-10", "2027-07-12", null));
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundable() {
        Quote q = quote("NON_REFUNDABLE", stay(201, "2027-03-10", "2027-03-12", null));
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.ZERO, RatePlans.find("NON_REFUNDABLE").refund(Money.of("662.83")));
    }

    @Test
    void corporate() {
        Quote q = quote("CORPORATE", stay(201, "2027-03-10", "2027-03-12", "TECNOSUL"));
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("93.58"), q.discount());
        assertEquals(Money.of("686.22"), q.total());
        assertThrows(DomainException.class, () -> quote("CORPORATE", stay(201, "2027-03-10", "2027-03-12", null)));
        assertThrows(DomainException.class, () -> quote("CORPORATE", stay(201, "2027-03-10", "2027-03-12", "OUTRA")));
    }

    @Test
    void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", stay(201, "2027-03-12", "2027-03-14", null));
        assertEquals(Money.of("820.00"), q.total());
        assertEquals(Money.ZERO, q.discount());
        assertEquals(Money.of("410.00"), RatePlans.find("WEEKEND_PACKAGE").refund(Money.of("820.00")));
        assertThrows(DomainException.class, () -> quote("WEEKEND_PACKAGE", stay(201, "2027-03-11", "2027-03-13", null)));
        assertThrows(DomainException.class, () -> quote("WEEKEND_PACKAGE", stay(201, "2027-03-12", "2027-03-15", null)));
    }

    @Test
    void longStay() {
        Quote q = quote("LONG_STAY", stay(101, "2027-03-01", "2027-03-08", null));
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(DomainException.class, () -> quote("LONG_STAY", stay(101, "2027-03-01", "2027-03-07", null)));
    }

    @Test
    void receiptText() {
        Stay stay = stay(201, "2027-03-10", "2027-03-12", "TECNOSUL");
        Quote q = quote("CORPORATE", stay);
        Receipt receipt = new Receipt(7, "Carla Dias", "111.222.333-44", 201, stay.room().type(),
                stay.checkIn(), stay.checkOut(), 2, q.subtotal(), q.discount(), q.total());
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Desconto: R$ 93,58
                Total: R$ 686,22""", receipt.render());
    }
}
