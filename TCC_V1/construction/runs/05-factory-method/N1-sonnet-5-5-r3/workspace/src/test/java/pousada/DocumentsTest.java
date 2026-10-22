package pousada;

import org.junit.jupiter.api.Test;
import pousada.domain.*;
import pousada.identity.*;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentsTest {

    private static Reservation paid(long id, int room, String name, GuestIdentity identity, String in, String out,
                                    RatePlan plan) {
        NewReservation n = new NewReservation(room, name, "x@y.com", identity, LocalDate.parse(in),
                LocalDate.parse(out), 2, plan);
        return Reservation.pending(Rooms.find(room).orElseThrow(), n).withId(id).confirm();
    }

    @Test
    void highSeasonPricing() {
        Quote q = Quote.of(Rooms.find(101).orElseThrow(), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(Money.of("599.76"), q.subtotal());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = Quote.of(Rooms.find(301).orElseThrow(), LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-11"), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("579.90"), q.subtotal());
        assertEquals(Money.of("86.98"), q.discount()); // 86.985 -> par (8 é par)
        assertEquals(Money.of("492.92"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-12-15")));
        assertTrue(Season.isHigh(LocalDate.parse("2028-02-29")));
        assertFalse(Season.isHigh(LocalDate.parse("2028-03-01")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-06-30")));
    }

    @Test
    void comprovante() {
        Reservation r = paid(7, 201, "Carla Dias", new CpfIdentity("111.222.333-44"), "2027-03-10", "2027-03-12",
                RatePlan.STANDARD);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", ReservationDocument.text(r));
    }

    @Test
    void faturaComIss() {
        Reservation r = paid(3, 201, "Ana Souza", new CnpjIdentity("12.345.678/0001-90", "Tecnosul Ltda"),
                "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", ReservationDocument.text(r));
    }

    @Test
    void bookingReceiptWithDiscountAndOneNight() {
        Reservation r = paid(4, 201, "John Smith", new PassportIdentity("X1234567", "United Kingdom"),
                "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE);
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42""", ReservationDocument.text(r));
    }

    @Test
    void thousandsSeparators() {
        assertEquals("R$ 1.234,56", DocumentLanguage.PORTUGUESE.money(Money.of("1234.56")));
        assertEquals("BRL 1,234.56", DocumentLanguage.ENGLISH.money(Money.of("1234.56")));
    }

    @Test
    void emailWrapsDocument() {
        Reservation r = paid(7, 201, "Carla Dias", new CpfIdentity("111.222.333-44"), "2027-03-10", "2027-03-12",
                RatePlan.STANDARD);
        Message m = ConfirmationEmail.of(r);
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.subject());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. "
                + "Estes são os dados dela:\n\n" + ReservationDocument.text(r)
                + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", m.body());
    }

    @Test
    void identityNeedsExactlyOneKind() {
        assertThrows(BusinessException.class, () -> IdentityKind.fromValues(Map.of()));
        assertThrows(BusinessException.class, () -> IdentityKind.fromValues(Map.of("companyCnpj", "1")));
        assertThrows(BusinessException.class,
                () -> IdentityKind.fromValues(Map.of("guestCpf", "1", "guestPassport", "2", "guestCountry", "BR")));
        assertInstanceOf(CpfIdentity.class, IdentityKind.fromValues(Map.of("guestCpf", "1")));
    }

    @Test
    void cancelRefunds() {
        GuestIdentity id = new CpfIdentity("1");
        Reservation std = paid(1, 101, "A", id, "2027-03-10", "2027-03-12", RatePlan.STANDARD).cancel();
        assertEquals(std.amountPaid(), std.refund());
        Reservation nr = paid(2, 101, "A", id, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE).cancel();
        assertEquals(Money.ZERO, nr.refund());
        assertEquals(nr.amountPaid(), nr.retained());
        NewReservation n = new NewReservation(101, "A", "a@b", id, LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), 1, RatePlan.STANDARD);
        assertEquals(Money.ZERO, Reservation.pending(Rooms.find(101).orElseThrow(), n).cancel().refund());
    }
}
