package pousada;

import org.junit.jupiter.api.Test;
import pousada.document.IssuedDocument;
import pousada.domain.Money;
import pousada.domain.Quote;
import pousada.domain.RatePlan;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.domain.Season;
import pousada.document.Stay;
import pousada.domain.RoomType;
import pousada.identity.CnpjIdentity;
import pousada.identity.CpfIdentity;
import pousada.identity.PassportIdentity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingAndDocumentsTest {

    @Test
    void highSeasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.of(2027, 12, 14)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 12, 15)));
        assertTrue(Season.isHigh(LocalDate.of(2028, 2, 29)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 3, 1)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 7, 31)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void highSeasonStandardQuote() {
        Room room = RoomCatalog.require(101);
        Quote quote = Quote.of(room, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, quote.nights());
        assertEquals(Money.of("599.76"), quote.total());
        assertEquals(Money.of("0.00"), quote.discount());
    }

    @Test
    void nonRefundableDiscountIsRoundedHalfEven() {
        Room room = RoomCatalog.require(301);
        Quote quote = Quote.of(room, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("579.90"), quote.subtotal());
        assertEquals(Money.of("86.98"), quote.discount()); // 86.985 -> par
        assertEquals(Money.of("492.92"), quote.total());
    }

    private Stay stay(int id, String guest, int nights, String subtotal, String discount, String total) {
        return new Stay(id, guest, 201, RoomType.DELUXE, LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 10).plusDays(nights), nights,
                Money.of(subtotal), Money.of(discount), Money.of(total));
    }

    @Test
    void comprovante() {
        IssuedDocument doc = new CpfIdentity("111.222.333-44")
                .issue(stay(7, "Carla Dias", 2, "779.80", "0", "779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", doc.text());
        assertEquals("Comprovante de reserva", doc.title());
    }

    @Test
    void comprovanteWithDiscountAndOneNight() {
        IssuedDocument doc = new CpfIdentity("1").issue(stay(8, "A", 1, "1234.56", "185.18", "1049.38"));
        assertTrue(doc.text().contains("1 diária\nDiárias: R$ 1.234,56\nDesconto: R$ 185,18\nTotal: R$ 1.049,38"));
    }

    @Test
    void fatura() {
        IssuedDocument doc = new CnpjIdentity("12.345.678/0001-90", "Tecnosul Ltda")
                .issue(stay(3, "Ana Souza", 2, "779.80", "0", "779.80"));
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", doc.text());
    }

    @Test
    void bookingReceipt() {
        IssuedDocument doc = new PassportIdentity("X1234567", "United Kingdom")
                .issue(stay(4, "John Smith", 2, "779.80", "0", "779.80"));
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80""", doc.text());
    }

    @Test
    void bookingReceiptWithDiscountAndOneNight() {
        IssuedDocument doc = new PassportIdentity("X", "UK").issue(stay(5, "J", 1, "1234.56", "185.18", "1049.38"));
        assertTrue(doc.text().contains("1 night\nNights: BRL 1,234.56\nDiscount: BRL 185.18\nTotal: BRL 1,049.38"));
    }
}
