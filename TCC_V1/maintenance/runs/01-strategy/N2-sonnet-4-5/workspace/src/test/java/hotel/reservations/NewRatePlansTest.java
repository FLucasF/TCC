package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Tests for the new rate plans, validating the specific calculations provided. */
class NewRatePlansTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(new SentMessages()));

    @Test
    void corporateTecnosulDeluxeMarch10to12() {
        // "No convênio da Tecnosul, um deluxe de 10/03/2027 a 12/03/2027 dá R$ 779,80 de diárias,
        //  R$ 93,58 de desconto e R$ 686,22 de total."
        Guest guest = new Guest("Corporate Guest", "corporate@example.com", "123.456.789-00");
        NewReservation reservation = new NewReservation(
                201, guest, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
                RatePlan.CORPORATE, "TECNOSUL");

        Reservation created = service.create(reservation);

        assertEquals(new BigDecimal("779.80"), created.price().subtotal());
        assertEquals(new BigDecimal("93.58"), created.price().discount());
        assertEquals(new BigDecimal("686.22"), created.price().total());
    }

    @Test
    void weekendPackageDeluxeMarch12to14() {
        // "O pacote no deluxe, de sexta 12/03/2027 a domingo 14/03/2027, sai R$ 820,00,
        //  e se a pessoa pagar e depois cancelar, volta R$ 410,00."
        Guest guest = new Guest("Weekend Guest", "weekend@example.com", "123.456.789-00");
        NewReservation reservation = new NewReservation(
                201, guest, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), 2,
                RatePlan.WEEKEND_PACKAGE, null);

        Reservation created = service.create(reservation);
        assertEquals(new BigDecimal("820.00"), created.price().total());

        service.confirm(created.id());
        service.cancel(created.id());
        assertEquals(new BigDecimal("410.00"), created.refund());
    }

    @Test
    void longStayStandardMarch1to8() {
        // "E a estadia longa num standard de 01/03/2027 a 08/03/2027 dá R$ 1.749,30 de diárias,
        //  R$ 174,93 de desconto e R$ 1.574,37 de total."
        Guest guest = new Guest("Long Stay Guest", "longstay@example.com", "123.456.789-00");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), 2,
                RatePlan.LONG_STAY, null);

        Reservation created = service.create(reservation);

        assertEquals(new BigDecimal("1749.30"), created.price().subtotal());
        assertEquals(new BigDecimal("174.93"), created.price().discount());
        assertEquals(new BigDecimal("1574.37"), created.price().total());
    }

    @Test
    void corporateValidatesCompanyCode() {
        Guest guest = new Guest("Guest", "guest@example.com", "123.456.789-00");

        // Missing company code
        NewReservation noCode = new NewReservation(
                101, guest, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
                RatePlan.CORPORATE, null);
        HotelException ex = assertThrows(HotelException.class, () -> service.create(noCode));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());

        // Invalid company code
        NewReservation badCode = new NewReservation(
                101, guest, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
                RatePlan.CORPORATE, "INVALIDCOMPANY");
        ex = assertThrows(HotelException.class, () -> service.create(badCode));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }

    @Test
    void weekendPackageValidatesFridayToSunday() {
        Guest guest = new Guest("Guest", "guest@example.com", "123.456.789-00");

        // Not Friday
        NewReservation notFriday = new NewReservation(
                101, guest, LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 15), 2,
                RatePlan.WEEKEND_PACKAGE, null);
        HotelException ex = assertThrows(HotelException.class, () -> service.create(notFriday));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());

        // Not Sunday
        NewReservation notSunday = new NewReservation(
                101, guest, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15), 2,
                RatePlan.WEEKEND_PACKAGE, null);
        ex = assertThrows(HotelException.class, () -> service.create(notSunday));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }

    @Test
    void longStayRequiresSevenNights() {
        Guest guest = new Guest("Guest", "guest@example.com", "123.456.789-00");

        // Only 6 nights
        NewReservation sixNights = new NewReservation(
                101, guest, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7), 2,
                RatePlan.LONG_STAY, null);
        HotelException ex = assertThrows(HotelException.class, () -> service.create(sixNights));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }
}
