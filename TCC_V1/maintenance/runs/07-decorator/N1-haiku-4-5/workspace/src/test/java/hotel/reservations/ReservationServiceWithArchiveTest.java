package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.messages.EmailConfig;
import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationServiceWithArchiveTest {

    private final SentMessages sentMessages = new SentMessages(new EmailConfig(false, "arquivo@pousada.com.br"));
    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages));

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);
    }

    @Test
    void confirmationCreatesArchiveCopy() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));

        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(2, sent.size());

        Message original = sent.get(0);
        assertEquals("ana@example.com", original.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", original.subject());
        assertEquals("SENT", original.status().name());

        Message archive = sent.get(1);
        assertEquals("arquivo@pousada.com.br", archive.to());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", archive.subject());
        assertEquals(original.body(), archive.body());
        assertEquals("SENT", archive.status().name());
    }
}
