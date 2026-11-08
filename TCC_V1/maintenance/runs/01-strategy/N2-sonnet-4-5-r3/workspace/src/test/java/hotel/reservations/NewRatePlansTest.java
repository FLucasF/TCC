package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NewRatePlansTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            new EmailSender(new SentMessages(database.dataSource())));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void tecnosulCorporateExample() {
        Guest guest = new Guest("Cliente Tecnosul", "cliente@tecnosul.com", "111.222.333-44");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.CORPORATE, "TECNOSUL");

        Reservation reservation = service.create(request);

        assertEquals(new BigDecimal("779.80"), reservation.price().subtotal());
        assertEquals(new BigDecimal("93.58"), reservation.price().discount());
        assertEquals(new BigDecimal("686.22"), reservation.price().total());
    }

    @Test
    void weekendPackageDeluxeExample() {
        Guest guest = new Guest("Cliente Fim de Semana", "cliente@example.com", "111.222.333-44");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), 2, RatePlan.WEEKEND_PACKAGE, null);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        Reservation cancelled = service.cancel(reservation.id());

        assertEquals(new BigDecimal("820.00"), reservation.price().total());
        assertEquals(new BigDecimal("410.00"), cancelled.refund());
    }

    @Test
    void longStayStandardExample() {
        Guest guest = new Guest("Cliente Estadia Longa", "cliente@example.com", "111.222.333-44");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), 2, RatePlan.LONG_STAY, null);

        Reservation reservation = service.create(request);

        assertEquals(new BigDecimal("1749.30"), reservation.price().subtotal());
        assertEquals(new BigDecimal("174.93"), reservation.price().discount());
        assertEquals(new BigDecimal("1574.37"), reservation.price().total());
    }
}
