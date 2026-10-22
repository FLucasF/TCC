package br.com.marealta;

import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Room;
import br.com.marealta.identity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuoteAndDocumentTest {
    private final Room standard = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
    private final Room deluxe = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Quote q = Quote.of(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        // 14/12 normal, 15/12 alta
        Quote q = Quote.of(standard, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = Quote.of(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    private DocumentData data(IdentityKind kind, Map<String, String> values, Quote quote) {
        return new DocumentData(7, "Carla Dias", new GuestIdentity(kind, values), 201, "DELUXE",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), quote);
    }

    private final Quote plain = Quote.of(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD);

    @Test
    void cpfReceipt() {
        IdentityKind kind = IdentityKinds.resolve(java.util.Set.of("guestCpf")).orElseThrow();
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", DocumentRenderer.render(data(kind, Map.of("guestCpf", "111.222.333-44"), plain)));
    }

    @Test
    void invoiceWithIss() {
        IdentityKind kind = IdentityKinds.resolve(java.util.Set.of("companyCnpj", "companyName")).orElseThrow();
        String text = DocumentRenderer.render(new DocumentData(3, "Ana Souza", new GuestIdentity(kind,
                Map.of("companyCnpj", "12.345.678/0001-90", "companyName", "Tecnosul Ltda")), 201, "DELUXE",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), plain));
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", text);
    }

    @Test
    void bookingReceiptWithDiscountAndOneNight() {
        IdentityKind kind = IdentityKinds.resolve(java.util.Set.of("guestPassport", "guestCountry")).orElseThrow();
        Quote one = Quote.of(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        String text = DocumentRenderer.render(new DocumentData(4, "John Smith", new GuestIdentity(kind,
                Map.of("guestPassport", "X1234567", "guestCountry", "United Kingdom")), 201, "DELUXE",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), one));
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42""", text);
    }

    @Test
    void thousandsSeparators() {
        assertEquals("R$ 1.234,56", Language.PT_BR.money(new BigDecimal("1234.56")));
        assertEquals("BRL 1,234.56", Language.EN.money(new BigDecimal("1234.56")));
    }
}
