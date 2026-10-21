package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class EmailRulesTest {

    @Test
    void invalidEmailIsBlocked() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages, false));

        Guest guest = new Guest("Ana Souza", "ana@gmail", "123.456.789-09");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        service.create(reservation);
        service.confirm(1L);

        List<Message> sent = sentMessages.all();
        assertEquals(1, sent.size());
        assertEquals(MessageStatus.BLOCKED, sent.get(0).status());
        assertEquals("ana@gmail", sent.get(0).to());
    }

    @Test
    void anotherInvalidEmailIsBlocked() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages, false));

        Guest guest = new Guest("Ana Souza", "ana.gmail.com", "123.456.789-09");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        service.create(reservation);
        service.confirm(1L);

        List<Message> sent = sentMessages.all();
        assertEquals(1, sent.size());
        assertEquals(MessageStatus.BLOCKED, sent.get(0).status());
        assertEquals("ana.gmail.com", sent.get(0).to());
    }

    @Test
    void validEmailIsSent() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages, false));

        Guest guest = new Guest("Ana Souza", "ana@gmail.com", "123.456.789-09");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        service.create(reservation);
        service.confirm(1L);

        List<Message> sent = sentMessages.all();
        assertEquals(2, sent.size());
        assertEquals(MessageStatus.SENT, sent.get(0).status());
        assertEquals("ana@gmail.com", sent.get(0).to());
        assertEquals(MessageStatus.SENT, sent.get(1).status());
        assertEquals("arquivo@pousadamarealta.com.br", sent.get(1).to());
    }

    @Test
    void testModeRedirectsMessage() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages, true));

        Guest guest = new Guest("Ana Souza", "ana@gmail.com", "123.456.789-09");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        service.create(reservation);
        service.confirm(1L);

        List<Message> sent = sentMessages.all();
        assertEquals(1, sent.size());
        assertEquals(MessageStatus.SENT, sent.get(0).status());
        assertEquals("testes@pousadamarealta.com.br", sent.get(0).to());
        assertEquals("[TESTE] Reserva confirmada - Pousada Maré Alta", sent.get(0).subject());
        assertEquals(true, sent.get(0).body().startsWith("Destinatário original: ana@gmail.com\n"));
    }

    @Test
    void testModeDoesNotSendArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages, true));

        Guest guest = new Guest("Ana Souza", "ana@gmail.com", "123.456.789-09");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        service.create(reservation);
        service.confirm(1L);

        List<Message> sent = sentMessages.all();
        assertEquals(1, sent.size());
    }

    @Test
    void testModeBlocksInvalidEmail() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages, true));

        Guest guest = new Guest("Ana Souza", "ana@gmail", "123.456.789-09");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        service.create(reservation);
        service.confirm(1L);

        List<Message> sent = sentMessages.all();
        assertEquals(1, sent.size());
        assertEquals(MessageStatus.BLOCKED, sent.get(0).status());
        assertEquals("ana@gmail", sent.get(0).to());
    }

    @Test
    void blockedMessageDoesNotGenerateArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        ReservationService service = new ReservationService(
                new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages, false));

        Guest guest = new Guest("Ana Souza", "ana@gmail", "123.456.789-09");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        service.create(reservation);
        service.confirm(1L);

        List<Message> sent = sentMessages.all();
        assertEquals(1, sent.size());
    }
}
