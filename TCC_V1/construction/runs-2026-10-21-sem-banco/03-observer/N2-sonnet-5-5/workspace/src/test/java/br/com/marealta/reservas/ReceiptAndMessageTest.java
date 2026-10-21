package br.com.marealta.reservas;

import br.com.marealta.reservas.domain.*;
import br.com.marealta.reservas.notification.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceiptAndMessageTest {

    @Test
    void guestEmailMatchesSpecification() {
        MessageOutbox outbox = new MessageOutbox();
        ReservationService service = new ReservationService(RoomCatalog.marealta(),
                List.of(new GuestConfirmationEmail(outbox), new HousekeepingNotifier(outbox), new FinanceNotifier(outbox)));
        for (int i = 0; i < 6; i++) {
            service.create(new NewReservation(301, "X", "x@x", "1", LocalDate.of(2027, 1, 1 + 2 * i),
                    LocalDate.of(2027, 1, 2 + 2 * i), 1, RatePlan.STANDARD));
        }
        service.create(new NewReservation(201, "Carla Dias", "carla@x.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD));
        service.confirm("7");
        Message m = outbox.sent().get(0);
        assertEquals("carla@x.com", m.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.subject());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", m.body());
    }

    @Test
    void moneyFormatAndRounding() {
        assertEquals("R$ 1.234,56", Money.format(new java.math.BigDecimal("1234.56")));
        assertEquals(new java.math.BigDecimal("0.12"), Money.round(new java.math.BigDecimal("0.125")));
        assertEquals(new java.math.BigDecimal("0.14"), Money.round(new java.math.BigDecimal("0.135")));
    }
}
