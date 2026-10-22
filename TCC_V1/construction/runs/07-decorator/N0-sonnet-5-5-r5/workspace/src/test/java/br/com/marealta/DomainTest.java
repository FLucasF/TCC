package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static final BigDecimal STANDARD = new BigDecimal("249.90");

    @Test
    void highSeasonQuote() {
        var q = Pricing.quote(STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), "STANDARD");
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2028, 3, 1)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void nonRefundableDiscount() {
        var q = Pricing.quote(new BigDecimal("389.90"), LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), "NON_REFUNDABLE");
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void receiptText() {
        var r = new Receipt(Receipt.TITLE, 7, "Carla Dias", "111.222.333-44", 201, "DELUXE",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
                new BigDecimal("779.80"), Money.ZERO, new BigDecimal("779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Documents.receiptText(r));
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
    }

    @Test
    void emailValidation() {
        assertTrue(MessagePolicy.isValidEmail("ana@gmail.com"));
        assertFalse(MessagePolicy.isValidEmail("ana@gmail"));
        assertFalse(MessagePolicy.isValidEmail("ana.gmail.com"));
        assertFalse(MessagePolicy.isValidEmail("@gmail.com"));
        assertFalse(MessagePolicy.isValidEmail("a@@gmail.com"));
        assertFalse(MessagePolicy.isValidEmail("a@gmail."));
    }

    private static MessagePolicy policy(boolean test, MessagePolicy.Scope archive) {
        return new MessagePolicy(new MessagePolicy.Config(test, "testes@x.com.br", "arquivo@x.com.br",
                MessagePolicy.Scope.ALWAYS, archive, MessagePolicy.Scope.OFF, LocalTime.of(22, 0), LocalTime.of(7, 0),
                MessagePolicy.Scope.OFF, 3));
    }

    private static final MessagePolicy.Draft DRAFT = new MessagePolicy.Draft("ana@gmail.com", "Oi", "Texto");

    @Test
    void liveSendsWithArchiveCopy() {
        var out = policy(false, MessagePolicy.Scope.LIVE_ONLY).process(DRAFT, LocalTime.NOON, a -> 0);
        assertEquals(2, out.size());
        assertEquals("ana@gmail.com", out.get(0).to());
        assertEquals("arquivo@x.com.br", out.get(1).to());
        assertEquals("[Cópia] Oi", out.get(1).subject());
        assertEquals("Texto", out.get(1).body());
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        var out = policy(true, MessagePolicy.Scope.LIVE_ONLY).process(DRAFT, LocalTime.NOON, a -> 0);
        assertEquals(1, out.size());
        assertEquals("testes@x.com.br", out.get(0).to());
        assertEquals("[TESTE] Oi", out.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", out.get(0).body());
    }

    @Test
    void testModeNeverReachesRealAddressEvenWithCopyForced() {
        var out = policy(true, MessagePolicy.Scope.ALWAYS).process(DRAFT, LocalTime.NOON, a -> 0);
        assertTrue(out.stream().noneMatch(o -> o.blocked() || !o.to().equals("testes@x.com.br")));
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (boolean test : List.of(false, true)) {
            var out = policy(test, MessagePolicy.Scope.ALWAYS)
                    .process(new MessagePolicy.Draft("ana@gmail", "Oi", "T"), LocalTime.NOON, a -> 0);
            assertEquals(1, out.size());
            assertTrue(out.get(0).blocked());
            assertEquals("ana@gmail", out.get(0).to());
            assertEquals("Oi", out.get(0).subject());
        }
    }

    @Test
    void quietHoursAndDailyLimit() {
        var p = new MessagePolicy(new MessagePolicy.Config(false, "t@x.com.br", "a@x.com.br",
                MessagePolicy.Scope.ALWAYS, MessagePolicy.Scope.OFF, MessagePolicy.Scope.LIVE_ONLY,
                LocalTime.of(22, 0), LocalTime.of(7, 0), MessagePolicy.Scope.ALWAYS, 2));
        assertTrue(p.process(DRAFT, LocalTime.of(3, 0), a -> 0).get(0).blocked());
        assertTrue(p.process(DRAFT, LocalTime.of(23, 0), a -> 0).get(0).blocked());
        assertFalse(p.process(DRAFT, LocalTime.of(7, 0), a -> 1).get(0).blocked());
        assertTrue(p.process(DRAFT, LocalTime.of(12, 0), a -> 2).get(0).blocked());
    }
}
