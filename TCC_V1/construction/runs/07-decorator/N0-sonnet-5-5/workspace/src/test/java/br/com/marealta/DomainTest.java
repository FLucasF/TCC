package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import br.com.marealta.Config.Notifications;
import br.com.marealta.Config.Scope;
import br.com.marealta.Domain.RatePlan;
import br.com.marealta.Domain.Room;
import br.com.marealta.Domain.RoomType;
import br.com.marealta.Messaging.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainTest {
    static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    @Test
    void peakSeasonBoundaries() {
        assertFalse(Pricing.isPeakSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.isPeakSeason(LocalDate.of(2028, 3, 1)));
        assertTrue(Pricing.isPeakSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isPeakSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void peakQuote() {
        var q = Pricing.quote(STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.subtotal());
        assertEquals(new BigDecimal("0.00"), q.discount());
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        // 3 noites de 249.90 = 749.70; 15% = 112.455 -> 112.46 (meio para o par: ...5 com 5 antes -> 112.46)
        var q = Pricing.quote(STANDARD, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("749.70"), q.subtotal());
        assertEquals(new BigDecimal("112.46"), q.discount());
        assertEquals(new BigDecimal("637.24"), q.total());
    }

    @Test
    void stayCrossingIntoPeakPricesEachNight() {
        // 14/12 normal, 15/12 alta
        var q = Pricing.quote(DELUXE, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("389.90").add(new BigDecimal("467.88")), q.subtotal());
    }

    @Test
    void receiptText() {
        var r = new Receipt(7, "Carla Dias", "111.222.333-44", 201, RoomType.DELUXE,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
                new BigDecimal("779.80"), new BigDecimal("0.00"), new BigDecimal("779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.render());
        assertEquals("R$ 1.234.567,05", Receipt.brl(new BigDecimal("1234567.05")));
        var d = new Receipt(1, "A", "B", 101, RoomType.STANDARD, LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 11), 1, new BigDecimal("100.00"), new BigDecimal("15.00"), new BigDecimal("85.00"));
        assertTrue(d.render().contains("| 1 diária\nDiárias: R$ 100,00\nDesconto: R$ 15,00\nTotal: R$ 85,00"));
    }

    @Test
    void addressValidation() {
        assertTrue(Messaging.isValidAddress("ana@gmail.com"));
        assertTrue(Messaging.isValidAddress("a@b.c.d"));
        for (String bad : new String[] {"ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com",
                "ana@.com", "ana@gmail.", "ana@gm..com", "", "a b@c.com"}) {
            assertFalse(Messaging.isValidAddress(bad), bad);
        }
    }

    // ---------- cadeia de regras ----------

    static final ZonedDateTime NOON = ZonedDateTime.of(2027, 3, 1, 12, 0, 0, 0, ZoneId.of("America/Sao_Paulo"));
    static final Draft DRAFT = new Draft("ana@gmail.com", "Assunto", "Corpo");

    static List<Outgoing> run(Notifications n, boolean test, Draft d, ZonedDateTime now, int sentToday) {
        return new Pipeline(n, test).process(d, new Context(test, now, a -> sentToday));
    }

    @Test
    void liveSendsWithArchiveCopy() {
        var out = run(Notifications.defaults(), false, DRAFT, NOON, 0);
        assertEquals(2, out.size());
        assertEquals("ana@gmail.com", out.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", out.get(1).to());
        assertEquals("[Cópia] Assunto", out.get(1).subject());
        assertEquals("Corpo", out.get(1).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (boolean test : new boolean[] {false, true}) {
            var out = run(Notifications.defaults(), test, new Draft("ana@gmail", "S", "B"), NOON, 0);
            assertEquals(1, out.size());
            assertEquals(MessageStatus.BLOCKED, out.get(0).status());
            assertEquals("ana@gmail", out.get(0).to());
            assertEquals("S", out.get(0).subject());
        }
    }

    @Test
    void testModeRedirectsEverything() {
        var out = run(Notifications.defaults(), true, DRAFT, NOON, 0);
        assertEquals(1, out.size());
        assertEquals("testes@pousadamarealta.com.br", out.get(0).to());
        assertEquals("[TESTE] Assunto", out.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nCorpo", out.get(0).body());
    }

    @Test
    void testModeNeverLeaksEvenWithArchiveCopyEnabled() {
        var n = withScopes(Scope.ALWAYS, Scope.ALWAYS, Scope.NEVER, Scope.NEVER);
        var out = run(n, true, DRAFT, NOON, 0);
        assertEquals(2, out.size());
        for (Outgoing m : out) assertEquals("testes@pousadamarealta.com.br", m.to());
        assertEquals("[TESTE] [Cópia] Assunto", out.get(1).subject());
        assertEquals("Destinatário original: arquivo@pousadamarealta.com.br\nCorpo", out.get(1).body());
    }

    @Test
    void archiveCopyCanBeDisabled() {
        var n = withScopes(Scope.ALWAYS, Scope.NEVER, Scope.NEVER, Scope.NEVER);
        assertEquals(1, run(n, false, DRAFT, NOON, 0).size());
    }

    @Test
    void quietHoursBlock() {
        var n = withScopes(Scope.ALWAYS, Scope.LIVE_ONLY, Scope.ALWAYS, Scope.NEVER);
        var night = NOON.withHour(2);
        var out = run(n, false, DRAFT, night, 0);
        assertEquals(1, out.size());
        assertEquals(MessageStatus.BLOCKED, out.get(0).status());
        assertEquals(MessageStatus.SENT, run(n, false, DRAFT, NOON, 0).get(0).status());
        assertEquals(MessageStatus.SENT, run(n, false, DRAFT, NOON.withHour(7), 0).get(0).status());
        assertEquals(MessageStatus.BLOCKED, run(n, false, DRAFT, NOON.withHour(22), 0).get(0).status());
        // só em modo de teste: fora dele não bloqueia
        var testOnly = withScopes(Scope.ALWAYS, Scope.LIVE_ONLY, Scope.TEST_ONLY, Scope.NEVER);
        assertEquals(MessageStatus.SENT, run(testOnly, false, DRAFT, night, 0).get(0).status());
        assertEquals(MessageStatus.BLOCKED, run(testOnly, true, DRAFT, night, 0).get(0).status());
    }

    @Test
    void dailyLimitBlocks() {
        var n = withScopes(Scope.ALWAYS, Scope.LIVE_ONLY, Scope.NEVER, Scope.LIVE_ONLY); // limite 2
        assertEquals(MessageStatus.SENT, run(n, false, DRAFT, NOON, 1).get(0).status());
        assertEquals(MessageStatus.BLOCKED, run(n, false, DRAFT, NOON, 2).get(0).status());
        assertEquals(1, run(n, false, DRAFT, NOON, 2).size());
        assertEquals(MessageStatus.SENT, run(n, true, DRAFT, NOON, 2).get(0).status());
    }

    static Notifications withScopes(Scope validation, Scope archive, Scope quiet, Scope limit) {
        var d = Notifications.defaults();
        return new Notifications(validation, archive, quiet, limit, d.archiveAddress(), d.testAddress(),
                LocalTime.of(22, 0), LocalTime.of(7, 0), 2);
    }
}
