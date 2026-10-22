package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import br.com.marealta.model.Room;
import br.com.marealta.plan.*;
import br.com.marealta.service.Receipt;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RatePlanTest {
    static final Room STANDARD = new Room(101, "STANDARD", 2, Money.of("249.90"));
    static final Room DELUXE = new Room(201, "DELUXE", 3, Money.of("389.90"));

    static Quote quote(String plan, Room room, String in, String out, String company) {
        return RatePlans.find(plan).orElseThrow()
                .quote(new QuoteRequest(room, LocalDate.parse(in), LocalDate.parse(out), company));
    }

    static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(expected, actual.toPlainString());
    }

    @Test
    void highSeasonNights() {
        Quote q = quote("STANDARD", STANDARD, "2027-07-10", "2027-07-12", null);
        assertMoney("599.76", q.subtotal());
        assertMoney("599.76", q.total());
        assertTrue(Season.isHigh(LocalDate.parse("2027-12-15")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2028-02-29")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-03-01")));
    }

    @Test
    void nonRefundable() {
        Quote q = quote("NON_REFUNDABLE", DELUXE, "2027-03-10", "2027-03-12", null);
        assertMoney("779.80", q.subtotal());
        assertMoney("116.97", q.discount());
        assertMoney("662.83", q.total());
        assertMoney("0.00", RatePlans.find("NON_REFUNDABLE").orElseThrow().refund(q.total()));
    }

    @Test
    void corporate() {
        Quote q = quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertMoney("779.80", q.subtotal());
        assertMoney("93.58", q.discount());
        assertMoney("686.22", q.total());
        assertEquals(400, assertThrows(ApiException.class, () -> quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", null)).status());
        assertEquals(400, assertThrows(ApiException.class, () -> quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "OUTRA")).status());
    }

    @Test
    void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-14", null);
        assertMoney("820.00", q.subtotal());
        assertMoney("0.00", q.discount());
        assertMoney("410.00", RatePlans.find("WEEKEND_PACKAGE").orElseThrow().refund(q.total()));
        // pacote tem preço fechado também na alta temporada
        assertMoney("520.00", quote("WEEKEND_PACKAGE", STANDARD, "2027-07-09", "2027-07-11", null).total());
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", DELUXE, "2027-03-13", "2027-03-15", null));
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void longStay() {
        Quote q = quote("LONG_STAY", STANDARD, "2027-03-01", "2027-03-08", null);
        assertMoney("1749.30", q.subtotal());
        assertMoney("174.93", q.discount());
        assertMoney("1574.37", q.total());
        assertThrows(ApiException.class, () -> quote("LONG_STAY", STANDARD, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void receiptText() {
        Receipt r = new Receipt(7, "Carla Dias", "111.222.333-44", 201, "DELUXE", LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), 2, Money.of("779.80"), Money.of("0"), Money.of("779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.text());
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
        assertEquals("R$ 1.749.300,00", Money.brl(Money.of("1749300")));
    }
}
