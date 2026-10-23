package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import hotel.Hotel;
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

class TestModeIntegrationTest {

    private final EmptyDatabase database = new EmptyDatabase();

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void testModeRedirectsMessagesToTestAddress() {
        Hotel hotel = new Hotel(database.dataSource(), true); // test mode ON
        Rooms rooms = new Rooms(database.dataSource());
        ReservationStore store = new ReservationStore(database.dataSource());
        SentMessages sentMessages = new SentMessages(database.dataSource());
        FilterChain filterChain = new FilterChain(List.of(new EmailValidator(), new TestModeFilter()));
        ReservationService service = new ReservationService(
                rooms, store, new PriceCalculator(), new EmailSender(sentMessages, filterChain));

        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        Reservation reservation = service.create(request);
        service.confirm(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Reserva confirmada - Pousada Maré Alta", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: ana@example.com\n"));
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        hotel.stop();
    }

    @Test
    void normalModeGeneratesArchiveCopy() {
        Hotel hotel = new Hotel(database.dataSource(), false); // test mode OFF
        Rooms rooms = new Rooms(database.dataSource());
        ReservationStore store = new ReservationStore(database.dataSource());
        SentMessages sentMessages = new SentMessages(database.dataSource());
        FilterChain filterChain = new FilterChain(List.of(new EmailValidator(), new ArchiveCopyFilter()));
        ReservationService service = new ReservationService(
                rooms, store, new PriceCalculator(), new EmailSender(sentMessages, filterChain));

        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        Reservation reservation = service.create(request);
        service.confirm(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).subject());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).subject());
        assertEquals(MessageStatus.SENT, messages.get(1).status());

        hotel.stop();
    }
}
