package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NewRatePlansTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(new SentMessages()));

    private static NewReservation stay(
            int roomNumber, String checkIn, String checkOut, RatePlan plan, String companyCode) {
        Guest guest = new Guest("João Silva", "joao@example.com", "111.222.333-44");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 2, plan, companyCode);
    }

    @Test
    void corporateRateAppliesCompanyDiscount() {
        Reservation tecnosul = service.create(stay(202, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));
        Reservation agrovale = service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE"));
        Reservation farmavida = service.create(stay(101, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA"));

        // 2 nights standard 101: 2 * 249.90 = 499.80
        assertEquals(new BigDecimal("499.80"), farmavida.price().subtotal());
        assertEquals(new BigDecimal("124.95"), farmavida.price().discount()); // 25%
        assertEquals(new BigDecimal("374.85"), farmavida.price().total());

        // 2 nights deluxe 202: 2 * 389.90 = 779.80
        assertEquals(new BigDecimal("779.80"), tecnosul.price().subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.price().discount()); // 12%
        assertEquals(new BigDecimal("686.22"), tecnosul.price().total());

        // 2 nights deluxe 201: 2 * 389.90 = 779.80
        assertEquals(new BigDecimal("779.80"), agrovale.price().subtotal());
        assertEquals(new BigDecimal("140.36"), agrovale.price().discount()); // 18%
        assertEquals(new BigDecimal("639.44"), agrovale.price().total());
    }

    @Test
    void corporateRateRequiresValidCompanyCode() {
        HotelException noCode = assertThrows(
                HotelException.class,
                () -> service.create(stay(101, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST, noCode.errorCode());

        HotelException invalidCode = assertThrows(
                HotelException.class,
                () -> service.create(stay(101, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "OUTRACORP")));
        assertEquals(ErrorCode.INVALID_REQUEST, invalidCode.errorCode());
    }

    @Test
    void corporateRateRefundsFullAmount() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(reservation.price().total(), reservation.refund());
    }

    @Test
    void weekendPackageHasFixedPriceByRoomType() {
        Reservation standard = service.create(stay(101, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));
        Reservation deluxe = service.create(stay(202, "2027-03-19", "2027-03-21", RatePlan.WEEKEND_PACKAGE, null));
        Reservation suite = service.create(stay(301, "2027-03-26", "2027-03-28", RatePlan.WEEKEND_PACKAGE, null));

        assertEquals(2, standard.price().nights());
        assertEquals(new BigDecimal("520.00"), standard.price().total());
        assertEquals(new BigDecimal("0.00"), standard.price().discount());

        assertEquals(2, deluxe.price().nights());
        assertEquals(new BigDecimal("820.00"), deluxe.price().total());
        assertEquals(new BigDecimal("0.00"), deluxe.price().discount());

        assertEquals(2, suite.price().nights());
        assertEquals(new BigDecimal("1200.00"), suite.price().total());
        assertEquals(new BigDecimal("0.00"), suite.price().discount());
    }

    @Test
    void weekendPackageRequiresFridayToSunday() {
        HotelException notFriday = assertThrows(
                HotelException.class,
                () -> service.create(stay(101, "2027-03-13", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST, notFriday.errorCode());

        HotelException notSunday = assertThrows(
                HotelException.class,
                () -> service.create(stay(101, "2027-03-12", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST, notSunday.errorCode());

        HotelException oneNight = assertThrows(
                HotelException.class,
                () -> service.create(stay(101, "2027-03-12", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST, oneNight.errorCode());
    }

    @Test
    void weekendPackageRefundsHalfAmount() {
        Reservation reservation = service.create(stay(202, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(new BigDecimal("410.00"), reservation.refund());
    }

    @Test
    void longStayApplies10PercentDiscount() {
        Reservation reservation = service.create(stay(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null));

        // 7 nights standard: 7 * 249.90 = 1749.30
        assertEquals(7, reservation.price().nights());
        assertEquals(new BigDecimal("1749.30"), reservation.price().subtotal());
        assertEquals(new BigDecimal("174.93"), reservation.price().discount());
        assertEquals(new BigDecimal("1574.37"), reservation.price().total());
    }

    @Test
    void longStayRequiresMinimum7Nights() {
        HotelException tooShort = assertThrows(
                HotelException.class,
                () -> service.create(stay(101, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null)));
        assertEquals(ErrorCode.INVALID_REQUEST, tooShort.errorCode());
    }

    @Test
    void longStayRefundsFullAmount() {
        Reservation reservation = service.create(stay(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(reservation.price().total(), reservation.refund());
    }

    @Test
    void longStayAppliesHighSeasonSurchargeToNightsInSeason() {
        Reservation reservation = service.create(stay(101, "2027-01-01", "2027-01-08", RatePlan.LONG_STAY, null));

        // 7 nights in high season: 7 * (249.90 * 1.20) = 7 * 299.88 = 2099.16
        assertEquals(new BigDecimal("2099.16"), reservation.price().subtotal());
        assertEquals(new BigDecimal("209.92"), reservation.price().discount());
        assertEquals(new BigDecimal("1889.24"), reservation.price().total());
    }
}
