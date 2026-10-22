package br.com.marealta;

import br.com.marealta.domain.CnpjIdentity;
import br.com.marealta.domain.CpfIdentity;
import br.com.marealta.domain.IdentityKinds;
import br.com.marealta.domain.PassportIdentity;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.ReceiptData;
import br.com.marealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {

    @Test
    void highSeasonQuote() {
        Quote q = Quote.of(RoomType.STANDARD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = Quote.of(RoomType.DELUXE, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    private ReceiptData data(long id, String name, BigDecimal discount) {
        BigDecimal sub = new BigDecimal("779.80");
        return new ReceiptData(id, name, 201, RoomType.DELUXE, LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), 2, sub, discount, sub.subtract(discount));
    }

    private static final BigDecimal NONE = new BigDecimal("0.00");

    @Test
    void comprovante() {
        var identity = IdentityKinds.from(Map.of("guestCpf", "111.222.333-44")).orElseThrow();
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", identity.document(data(7, "Carla Dias", NONE)).text());
    }

    @Test
    void fatura() {
        var identity = IdentityKinds.from(Map.of("companyCnpj", "12.345.678/0001-90",
                "companyName", "Tecnosul Ltda")).orElseThrow();
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", identity.document(data(3, "Ana Souza", NONE)).text());
    }

    @Test
    void bookingReceiptWithDiscount() {
        var identity = IdentityKinds.from(Map.of("guestPassport", "X1234567",
                "guestCountry", "United Kingdom")).orElseThrow();
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Discount: BRL 1,000.00
                Total: BRL -220.20""", identity.document(data(4, "John Smith", new BigDecimal("1000.00"))).text());
    }

    @Test
    void identityMustBeExactlyOneCompleteKind() {
        assertTrue(IdentityKinds.from(Map.of()).isEmpty());
        assertTrue(IdentityKinds.from(Map.of("companyCnpj", "1")).isEmpty());
        assertTrue(IdentityKinds.from(Map.of("guestCpf", "1", "guestPassport", "2", "guestCountry", "BR")).isEmpty());
        assertTrue(IdentityKinds.from(Map.of("guestCpf", "1")).get() instanceof CpfIdentity);
        assertTrue(IdentityKinds.from(Map.of("companyCnpj", "1", "companyName", "A")).get() instanceof CnpjIdentity);
        assertTrue(IdentityKinds.from(Map.of("guestPassport", "1", "guestCountry", "A")).get() instanceof PassportIdentity);
    }
}
