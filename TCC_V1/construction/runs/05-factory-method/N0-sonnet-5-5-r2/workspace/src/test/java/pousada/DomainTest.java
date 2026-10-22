package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static Reservation reservation(GuestIdentity id, int room, String in, String out, RatePlan plan, long n) {
        Room r = Room.find(room).orElseThrow();
        Quote q = Quote.of(r.type(), LocalDate.parse(in), LocalDate.parse(out), plan);
        return new Reservation(n, room, "Carla Dias", "c@x.com", id, LocalDate.parse(in), LocalDate.parse(out), 2,
                plan, q.nights(), q.subtotal(), q.discount(), q.total(), ReservationStatus.CONFIRMED, q.total(), null);
    }

    @Test
    void highSeasonPricing() {
        Quote q = Quote.of(RoomType.STANDARD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.STANDARD);
        assertEquals(Money.of("599.76"), q.total());
        // 14/12 normal, 15/12 high
        Quote edge = Quote.of(RoomType.STANDARD, LocalDate.parse("2027-12-14"), LocalDate.parse("2027-12-16"),
                RatePlan.STANDARD);
        assertEquals(Money.of("549.78"), edge.subtotal());
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        Quote q = Quote.of(RoomType.DELUXE, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.of("662.83"), q.total());
    }

    @Test
    void moneyFormats() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
        assertEquals("BRL 1,234.56", Money.brlEn(Money.of("1234.56")));
    }

    @Test
    void receipt() {
        Reservation r = reservation(new GuestIdentity.Individual("111.222.333-44"), 201, "2027-03-10", "2027-03-12",
                RatePlan.STANDARD, 7);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Documents.issue(r).text());
    }

    @Test
    void invoice() {
        Reservation r = reservation(new GuestIdentity.Company("12.345.678/0001-90", "Tecnosul Ltda"), 201,
                "2027-03-10", "2027-03-12", RatePlan.STANDARD, 3);
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Carla Dias
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", Documents.issue(r).text());
    }

    @Test
    void bookingReceiptWithDiscountAndSingleNight() {
        Reservation r = reservation(new GuestIdentity.Foreigner("X1234567", "United Kingdom"), 201,
                "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE, 4);
        assertEquals("""
                Booking receipt no. 4
                Guest: Carla Dias (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42""", Documents.issue(r).text());
    }
}
