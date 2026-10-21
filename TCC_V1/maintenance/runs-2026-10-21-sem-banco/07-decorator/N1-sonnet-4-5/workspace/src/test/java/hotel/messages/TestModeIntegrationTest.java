package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestModeIntegrationTest {

    @Test
    void demonstratesTestModeWithRedirection() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(),
                new ReservationStore(),
                new PriceCalculator(),
                new EmailSender(sentMessages, true)); // test mode enabled

        Guest guest = new Guest("Carlos Silva", "carlos@example.com", "123.456.789-00");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-06-10"), LocalDate.parse("2027-06-12"), 2, RatePlan.STANDARD);

        service.confirm(service.create(reservation).id());

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size()); // only redirected message, no archive copy

        Message message = messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", message.to());
        assertTrue(message.subject().startsWith("[TESTE] "));
        assertTrue(message.body().startsWith("Destinatário original: carlos@example.com\n"));
        assertEquals(MessageStatus.SENT, message.status());
    }

    @Test
    void demonstratesNormalModeWithArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(),
                new ReservationStore(),
                new PriceCalculator(),
                new EmailSender(sentMessages, false)); // normal mode

        Guest guest = new Guest("Maria Santos", "maria@example.com", "987.654.321-00");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-06-10"), LocalDate.parse("2027-06-12"), 2, RatePlan.STANDARD);

        service.confirm(service.create(reservation).id());

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size()); // original + archive copy

        Message original = messages.get(0);
        assertEquals("maria@example.com", original.to());
        assertEquals(MessageStatus.SENT, original.status());

        Message archive = messages.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", archive.to());
        assertTrue(archive.subject().startsWith("[Cópia] "));
        assertEquals(MessageStatus.SENT, archive.status());
    }

    @Test
    void demonstratesInvalidEmailBlocking() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("joao@invalid", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size()); // only blocked message, no archive

        Message message = messages.get(0);
        assertEquals("joao@invalid", message.to()); // keeps original invalid address
        assertEquals(MessageStatus.BLOCKED, message.status());
    }
}
