package pousada.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    private static Reservation pending(String plan) {
        return new Reservation(7, 201, "Carla Dias", "carla@example.com", "111.222.333-44",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, plan, 2,
                Money.of("779.80"), Money.ZERO, Money.of("779.80"), ReservationStatus.PENDING, Money.ZERO, null);
    }

    @Test
    void lifecycle() {
        Reservation r = pending("STANDARD").confirm();
        assertEquals(Money.of("779.80"), r.amountPaid());
        assertEquals(ReservationStatus.CHECKED_OUT, r.startStay().endStay().status());
        assertThrows(DomainException.class, r::confirm);
        assertThrows(DomainException.class, pending("STANDARD")::startStay);
    }

    @Test
    void cancellationRefunds() {
        assertEquals(Money.ZERO, pending("STANDARD").cancel().refund());
        assertEquals(Money.of("779.80"), pending("STANDARD").confirm().cancel().refund());
        assertEquals(Money.ZERO, pending("NON_REFUNDABLE").confirm().cancel().refund());
        assertThrows(DomainException.class, () -> pending("STANDARD").confirm().startStay().cancel());
    }

    @Test
    void emailBody() {
        Reservation r = pending("STANDARD").confirm();
        Message m = Message.confirmationEmail(r, r.receipt(Rooms.find(201).orElseThrow()));
        assertEquals("Olá, Carla Dias!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + "Comprovante de reserva nº 7\n"
                + "Hóspede: Carla Dias (CPF 111.222.333-44)\n"
                + "Quarto: 201 (DELUXE)\n"
                + "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\n"
                + "Diárias: R$ 779,80\n"
                + "Total: R$ 779,80\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!", m.body());
    }

    @Test
    void receiptNeedsPayment() {
        assertThrows(DomainException.class, () -> pending("STANDARD").receipt(Rooms.find(201).orElseThrow()));
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
    }
}
