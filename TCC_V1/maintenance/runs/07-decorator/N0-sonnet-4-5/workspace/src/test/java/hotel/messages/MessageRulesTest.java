package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageRulesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final Rooms rooms = new Rooms(database.dataSource());
    private final ReservationStore store = new ReservationStore(database.dataSource());
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation confirmReservation(ReservationService service, String email) {
        Guest guest = new Guest("Test User", email, "123.456.789-00");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 1, RatePlan.STANDARD);
        Reservation created = service.create(reservation);
        return service.confirm(created.id());
    }

    @Test
    void blocksInvalidEmail() {
        List<MessageRule> rules = List.of(new EmailValidationRule(new EmailValidator()));
        EmailSender sender = new EmailSender(sentMessages, rules);
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), sender);

        confirmReservation(service, "invalid@email");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals("invalid@email", messages.get(0).to());
    }

    @Test
    void sendsValidEmail() {
        List<MessageRule> rules = List.of(new EmailValidationRule(new EmailValidator()));
        EmailSender sender = new EmailSender(sentMessages, rules);
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), sender);

        confirmReservation(service, "valid@email.com");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("valid@email.com", messages.get(0).to());
    }

    @Test
    void redirectsToTestEmail() {
        List<MessageRule> rules = List.of(
                new EmailValidationRule(new EmailValidator()),
                new TestModeRule(true));
        EmailSender sender = new EmailSender(sentMessages, rules);
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), sender);

        confirmReservation(service, "original@email.com");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertTrue(messages.get(0).subject().startsWith("[TESTE] "));
        assertTrue(messages.get(0).body().startsWith("Destinatário original: original@email.com\n"));
    }

    @Test
    void createsArchiveCopy() {
        List<MessageRule> rules = List.of(
                new EmailValidationRule(new EmailValidator()),
                new TestModeRule(false),
                new ArchiveCopyRule(false));
        EmailSender sender = new EmailSender(sentMessages, rules);
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), sender);

        confirmReservation(service, "guest@email.com");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        Message original = messages.get(0);
        assertEquals(MessageStatus.SENT, original.status());
        assertEquals("guest@email.com", original.to());

        Message copy = messages.get(1);
        assertEquals(MessageStatus.SENT, copy.status());
        assertEquals("arquivo@pousadamarealta.com.br", copy.to());
        assertTrue(copy.subject().startsWith("[Cópia] "));
        assertEquals(original.body(), copy.body());
    }

    @Test
    void doesNotCreateArchiveCopyInTestMode() {
        List<MessageRule> rules = List.of(
                new EmailValidationRule(new EmailValidator()),
                new TestModeRule(true),
                new ArchiveCopyRule(true));
        EmailSender sender = new EmailSender(sentMessages, rules);
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), sender);

        confirmReservation(service, "guest@email.com");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
    }

    @Test
    void doesNotCreateArchiveCopyForBlockedEmail() {
        List<MessageRule> rules = List.of(
                new EmailValidationRule(new EmailValidator()),
                new TestModeRule(false),
                new ArchiveCopyRule(false));
        EmailSender sender = new EmailSender(sentMessages, rules);
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), sender);

        confirmReservation(service, "invalid@email");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals("invalid@email", messages.get(0).to());
    }

    @Test
    void blocksInvalidEmailInTestMode() {
        List<MessageRule> rules = List.of(
                new EmailValidationRule(new EmailValidator()),
                new TestModeRule(true));
        EmailSender sender = new EmailSender(sentMessages, rules);
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), sender);

        confirmReservation(service, "bad@email");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals("bad@email", messages.get(0).to());
    }
}
