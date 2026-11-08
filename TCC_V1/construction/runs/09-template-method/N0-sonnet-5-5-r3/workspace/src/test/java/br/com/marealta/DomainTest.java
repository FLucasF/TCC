package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.report.ReportRenderer;
import br.com.marealta.report.ReportTable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainTest {

    private static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    @Test
    void highSeasonStay() {
        Quote q = Quote.of(STANDARD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(new BigDecimal("0.00"), q.discount());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Quote.isHighSeason(LocalDate.parse("2026-12-14")));
        assertTrue(Quote.isHighSeason(LocalDate.parse("2026-12-15")));
        assertTrue(Quote.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Quote.isHighSeason(LocalDate.parse("2027-03-01")));
        assertFalse(Quote.isHighSeason(LocalDate.parse("2027-06-30")));
        assertTrue(Quote.isHighSeason(LocalDate.parse("2027-07-31")));
        assertFalse(Quote.isHighSeason(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        Quote q = Quote.of(STANDARD, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), RatePlan.NON_REFUNDABLE);
        // 249.90 * 0.15 = 37.485 -> 37.48 (half to even)
        assertEquals(new BigDecimal("37.48"), q.discount());
        assertEquals(new BigDecimal("212.42"), q.total());
    }

    @Test
    void moneyFormats() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.brl(BigDecimal.ZERO));
        assertEquals("600.00", Money.plain(new BigDecimal("600")));
    }

    @Test
    void receiptText() {
        Quote q = Quote.of(DELUXE, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), RatePlan.STANDARD);
        Reservation r = new Reservation(7, 201, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD, q.nights(),
                q.subtotal(), q.discount(), q.total(), ReservationStatus.CONFIRMED, q.total(), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, DELUXE).text());
    }

    @Test
    void rendererWritesTitleHeaderRowsTotal() {
        ReportTable t = new ReportTable("revenue", "2027-07", List.of("a", "b"), List.of(List.of("1", "x,y")),
                List.of("total", "1.00"));
        assertEquals("Pousada Maré Alta,revenue,2027-07\na,b\n1,\"x,y\"\ntotal,1.00\n", ReportRenderer.render(t));
    }
}
