package pousada;

import org.junit.jupiter.api.Test;
import pousada.Domain.RatePlan;
import pousada.Domain.RoomType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UnitTest {
    private static final java.math.BigDecimal STD = Money.of("249.90");

    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void quoteHighSeason() {
        var q = Pricing.quote(STD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals("599.76", q.total().toPlainString());
        assertEquals("0.00", q.discount().toPlainString());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        // 1 noite de 389.90 fora de temporada: 15% = 58.485 -> 58.48 (par)
        var q = Pricing.quote(Money.of("389.90"), LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        assertEquals("58.48", q.discount().toPlainString());
        assertEquals("331.42", q.total().toPlainString());
    }

    @Test
    void moneyFormats() {
        assertEquals("1.234,56", Money.br(Money.of("1234.56")));
        assertEquals("1,234.56", Money.en(Money.of("1234.56")));
        assertEquals("0,00", Money.br(Money.ZERO));
    }

    private static Identification.Receipt receipt(long id, java.math.BigDecimal sub, java.math.BigDecimal disc, int nights) {
        return new Identification.Receipt(id, "Carla Dias", 201, RoomType.DELUXE,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 10 + nights), nights, sub, disc, sub.subtract(disc));
    }

    @Test
    void comprovante() {
        String t = new Identification.Individual("111.222.333-44").render(receipt(7, Money.of("779.80"), Money.ZERO, 2));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", t);
    }

    @Test
    void faturaComIss() {
        var r = new Identification.Receipt(3, "Ana Souza", 201, RoomType.DELUXE,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, Money.of("779.80"), Money.ZERO, Money.of("779.80"));
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""",
                new Identification.Company("12.345.678/0001-90", "Tecnosul Ltda").render(r));
    }

    @Test
    void bookingReceiptWithDiscountAndSingleNight() {
        var r = new Identification.Receipt(4, "John Smith", 201, RoomType.DELUXE,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, Money.of("1234.56"), Money.of("100.00"), Money.of("1134.56"));
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 1,234.56
                Discount: BRL 100.00
                Total: BRL 1,134.56""",
                new Identification.Foreigner("X1234567", "United Kingdom").render(r));
    }
}
