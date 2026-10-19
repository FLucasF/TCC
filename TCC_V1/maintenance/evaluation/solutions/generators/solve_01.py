"""Scenario 01 (Strategy): the pattern solution (a RatePlanPolicy per plan, chosen through RatePlans; the price
and the refund ask the plan) and the naive one (more cases in the two switches, the plan rules as ifs in the
service)."""

from kit import Solution

# ---------------------------------------------------------------- the pattern: one policy per rate plan
pattern = Solution("01-pattern")
RESERVATIONS = "src/main/java/hotel/reservations/"

FILES: dict[str, str] = {}

FILES["RatePlan.java"] = '''package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** A discount agreed with the guest's company. */
    CORPORATE,
    /** Friday to Sunday for a fixed price per room type. */
    WEEKEND_PACKAGE,
    /** 7 nights or more, 10% off. */
    LONG_STAY
}
'''

FILES["NewReservation.java"] = '''package hotel.reservations;

import java.time.LocalDate;

/** companyCode is only used by the CORPORATE plan, and may be null. */
public record NewReservation(
        int roomNumber,
        Guest guest,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        String companyCode) {
}
'''

FILES["NightlyCharges.java"] = '''package hotel.reservations;

import java.math.BigDecimal;

/** The nights of a stay and what they cost at the room's rate, with the high-season surcharge. */
public record NightlyCharges(int nights, BigDecimal subtotal) {
}
'''

FILES["RatePlanPolicy.java"] = '''package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** What a rate plan decides: who may use it, what the stay costs, and what comes back on cancellation. */
public interface RatePlanPolicy {

    /** Throws INVALID_REQUEST when the plan does not allow this reservation. */
    void validate(Room room, NewReservation request);

    Price price(Room room, NewReservation request, NightlyCharges charges);

    BigDecimal refund(Reservation reservation);
}
'''

FILES["PercentOff.java"] = '''package hotel.reservations;

import java.math.BigDecimal;

final class PercentOff {

    private PercentOff() {
    }

    static Price of(NightlyCharges charges, BigDecimal rate) {
        BigDecimal discount = PriceCalculator.cents(charges.subtotal().multiply(rate));
        return new Price(charges.nights(), charges.subtotal(), discount, charges.subtotal().subtract(discount));
    }
}
'''

FILES["StandardPlan.java"] = '''package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class StandardPlan implements RatePlanPolicy {

    @Override
    public void validate(Room room, NewReservation request) {
    }

    @Override
    public Price price(Room room, NewReservation request, NightlyCharges charges) {
        return PercentOff.of(charges, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
'''

FILES["NonRefundablePlan.java"] = '''package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class NonRefundablePlan implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public void validate(Room room, NewReservation request) {
    }

    @Override
    public Price price(Room room, NewReservation request, NightlyCharges charges) {
        return PercentOff.of(charges, DISCOUNT);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
'''

FILES["CorporatePlan.java"] = '''package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/** The companies with an agreement, by code, and their discount. */
final class CorporatePlan implements RatePlanPolicy {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(Room room, NewReservation request) {
        if (request.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(request.companyCode())) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "the CORPORATE plan needs the code of a company with an agreement");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, NightlyCharges charges) {
        return PercentOff.of(charges, DISCOUNT_BY_COMPANY.get(request.companyCode()));
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
'''

FILES["WeekendPackagePlan.java"] = '''package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Friday to the Sunday right after, for a fixed price per room type, in any season. */
final class WeekendPackagePlan implements RatePlanPolicy {

    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal REFUNDED_SHARE = new BigDecimal("0.5");

    @Override
    public void validate(Room room, NewReservation request) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.checkOut().equals(request.checkIn().plusDays(2));
        if (!fridayToSunday) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from a Friday to the Sunday right after");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, NightlyCharges charges) {
        BigDecimal packagePrice = PRICE_BY_ROOM_TYPE.get(room.type());
        return new Price(charges.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(reservation.amountPaid().multiply(REFUNDED_SHARE));
    }
}
'''

FILES["LongStayPlan.java"] = '''package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/** 7 nights or more, 10% off the nights (high season included). */
final class LongStayPlan implements RatePlanPolicy {

    private static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void validate(Room room, NewReservation request) {
        if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < MINIMUM_NIGHTS) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay needs at least 7 nights");
        }
    }

    @Override
    public Price price(Room room, NewReservation request, NightlyCharges charges) {
        return PercentOff.of(charges, DISCOUNT);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
'''

FILES["RatePlans.java"] = '''package hotel.reservations;

import java.util.EnumMap;
import java.util.Map;

/** The rule of each rate plan. A new plan is a new constant in RatePlan and one line here. */
public final class RatePlans {

    private final Map<RatePlan, RatePlanPolicy> policies = new EnumMap<>(RatePlan.class);

    public RatePlans() {
        policies.put(RatePlan.STANDARD, new StandardPlan());
        policies.put(RatePlan.NON_REFUNDABLE, new NonRefundablePlan());
        policies.put(RatePlan.CORPORATE, new CorporatePlan());
        policies.put(RatePlan.WEEKEND_PACKAGE, new WeekendPackagePlan());
        policies.put(RatePlan.LONG_STAY, new LongStayPlan());
    }

    RatePlanPolicy policyFor(RatePlan ratePlan) {
        return policies.get(ratePlan);
    }
}
'''

FILES["PriceCalculator.java"] = '''package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

/**
 * The price of a stay: every night costs the room's nightly rate, plus the high-season surcharge on
 * high-season nights; the rate plan then decides the price from those nights. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    private final RatePlans ratePlans;

    public PriceCalculator(RatePlans ratePlans) {
        this.ratePlans = ratePlans;
    }

    /** Throws INVALID_REQUEST when the rate plan does not allow this stay. */
    public Price calculate(Room room, NewReservation request) {
        RatePlanPolicy plan = ratePlans.policyFor(request.ratePlan());
        plan.validate(room, request);
        return plan.price(room, request, nightlyCharges(room, request.checkIn(), request.checkOut()));
    }

    NightlyCharges nightlyCharges(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        return new NightlyCharges(nights, subtotal);
    }

    BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    /** High season: from December 15 to the end of February, and the whole of July. */
    static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
'''

for file_name, content in FILES.items():
    pattern.write(RESERVATIONS + file_name, content)

SERVICE = "src/main/java/hotel/reservations/ReservationService.java"
pattern.replace(SERVICE, """    private final PriceCalculator priceCalculator;
    private final EmailSender emailSender;

    public ReservationService(
            Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, EmailSender emailSender) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.emailSender = emailSender;
    }""", """    private final PriceCalculator priceCalculator;
    private final RatePlans ratePlans;
    private final EmailSender emailSender;

    public ReservationService(Rooms rooms, ReservationStore store, PriceCalculator priceCalculator,
            RatePlans ratePlans, EmailSender emailSender) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.ratePlans = ratePlans;
        this.emailSender = emailSender;
    }""")
pattern.replace(SERVICE, """        Price price = priceCalculator.calculate(room, request.checkIn(), request.checkOut(), request.ratePlan());""",
        """        Price price = priceCalculator.calculate(room, request);""")
pattern.replace(SERVICE, """        BigDecimal refund = switch (reservation.ratePlan()) {
            case STANDARD -> reservation.amountPaid();
            case NON_REFUNDABLE -> PriceCalculator.cents(BigDecimal.ZERO);
        };
        reservation.cancel(refund);""", """        BigDecimal refund = ratePlans.policyFor(reservation.ratePlan()).refund(reservation);
        reservation.cancel(refund);""")

REQUEST = "src/main/java/hotel/web/CreateReservationRequest.java"
pattern.replace(REQUEST, """/** The JSON body of POST /reservations. Every field is required. */""",
        """/** The JSON body of POST /reservations. Every field is required, but companyCode (CORPORATE only). */""")
pattern.replace(REQUEST, """        Integer guests,
        RatePlan ratePlan) {""", """        Integer guests,
        RatePlan ratePlan,
        String companyCode) {""")

API = "src/main/java/hotel/web/HttpApi.java"
pattern.replace(API, """        return new NewReservation(
                body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(), body.ratePlan());""",
        """        return new NewReservation(body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(),
                body.ratePlan(), body.companyCode());""")

HOTEL = "src/main/java/hotel/Hotel.java"
pattern.replace(HOTEL, "import hotel.reservations.PriceCalculator;\n",
        "import hotel.reservations.PriceCalculator;\nimport hotel.reservations.RatePlans;\n")
pattern.replace(HOTEL, """        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));""",
        """        RatePlans ratePlans = new RatePlans();
        ReservationService reservations = new ReservationService(
                rooms, store, new PriceCalculator(ratePlans), ratePlans, new EmailSender(sentMessages));""")

# the tests: the new constructors, and the calculator now takes the whole request
SERVICE_TEST = "src/test/java/hotel/reservations/ReservationServiceTest.java"
pattern.replace(SERVICE_TEST, """    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages));""",
        """    private final RatePlans ratePlans = new RatePlans();
    private final ReservationService service = new ReservationService(new Rooms(), new ReservationStore(),
            new PriceCalculator(ratePlans), ratePlans, new EmailSender(sentMessages));""")
pattern.replace(SERVICE_TEST, """        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);""",
        """        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan, null);""")

REPORTS_TEST = "src/test/java/hotel/reports/ReportsTest.java"
pattern.replace(REPORTS_TEST, "import hotel.reservations.RatePlan;\n",
        "import hotel.reservations.RatePlan;\nimport hotel.reservations.RatePlans;\n")
pattern.replace(REPORTS_TEST, """    private final ReservationService service =
            new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(new SentMessages()));""",
        """    private final RatePlans ratePlans = new RatePlans();
    private final ReservationService service = new ReservationService(
            rooms, store, new PriceCalculator(ratePlans), ratePlans, new EmailSender(new SentMessages()));""")
pattern.replace(REPORTS_TEST, """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan));""",
        """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan, null));""")

PRICE_TEST = "src/test/java/hotel/reservations/PriceCalculatorTest.java"
pattern.replace(PRICE_TEST, "private final PriceCalculator calculator = new PriceCalculator();",
                "private final PriceCalculator calculator = new PriceCalculator(new RatePlans());")
pattern.replace(PRICE_TEST, "calculator.calculate(\n                standardRoom, ", "price(\n                standardRoom, ",
                count=4)
pattern.replace(PRICE_TEST, "calculator.calculate(\n                suite, ", "price(\n                suite, ")
pattern.replace(PRICE_TEST, """    @Test
    void lowSeasonNightsCostTheNightlyRate() {""", """    private Price price(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return calculator.calculate(room, new NewReservation(room.number(), guest, checkIn, checkOut, 1, plan, null));
    }

    @Test
    void lowSeasonNightsCostTheNightlyRate() {""")

pattern.write("src/test/java/hotel/reservations/NewRatePlansTest.java", '''package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NewRatePlansTest {

    private final RatePlans ratePlans = new RatePlans();
    private final ReservationService service = new ReservationService(new Rooms(), new ReservationStore(),
            new PriceCalculator(ratePlans), ratePlans, new EmailSender(new SentMessages()));

    private Reservation book(int room, String checkIn, String checkOut, RatePlan plan, String companyCode) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                room, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan, companyCode));
    }

    @Test
    void corporateUsesTheCompanyDiscount() {
        Reservation reservation = book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("93.58"), reservation.price().discount());
        assertEquals(new BigDecimal("686.22"), reservation.price().total());
    }

    @Test
    void corporateNeedsAKnownCompany() {
        assertThrows(HotelException.class, () -> book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null));
        assertThrows(HotelException.class, () -> book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "ACME"));
    }

    @Test
    void theWeekendPackageIsAFixedPriceFromFridayToSunday() {
        Reservation reservation = book(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null);
        assertEquals(new BigDecimal("820.00"), reservation.price().total());
        service.confirm(reservation.id());

        assertEquals(new BigDecimal("410.00"), service.cancel(reservation.id()).refund());
        assertThrows(HotelException.class, () -> book(202, "2027-03-13", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null));
    }

    @Test
    void theLongStayNeedsSevenNights() {
        Reservation reservation = book(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null);

        assertEquals(new BigDecimal("174.93"), reservation.price().discount());
        assertEquals(new BigDecimal("1574.37"), reservation.price().total());
        assertThrows(HotelException.class, () -> book(102, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null));
    }
}
''')

pattern.build_and_save()

# ---------------------------------------------------------------- naive: more cases in the switches
naive = Solution("01-naive")

RESERVATIONS = "src/main/java/hotel/reservations/"

naive.replace(RESERVATIONS + "RatePlan.java", """    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE
}""", """    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** A discount agreed with the guest's company. */
    CORPORATE,
    /** Friday to Sunday for a fixed price per room type. */
    WEEKEND_PACKAGE,
    /** 7 nights or more, 10% off. */
    LONG_STAY
}""")

naive.replace(RESERVATIONS + "NewReservation.java", """        int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {""",
        """        int roomNumber, Guest guest, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
        String companyCode) {""")

CALCULATOR = RESERVATIONS + "PriceCalculator.java"
naive.replace(CALCULATOR, "import java.time.Month;\n", "import java.time.Month;\nimport java.util.Map;\n")
naive.replace(CALCULATOR, """    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {""",
        """    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");
    static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {""")
naive.replace(CALCULATOR, """        BigDecimal discount = switch (ratePlan) {
            case STANDARD -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        };
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));""",
        """        if (ratePlan == RatePlan.WEEKEND_PACKAGE) {
            BigDecimal packagePrice = switch (room.type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            };
            return new Price(nights, packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }
        BigDecimal discount = switch (ratePlan) {
            case STANDARD -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
            case CORPORATE -> cents(subtotal.multiply(COMPANY_DISCOUNTS.get(companyCode)));
            case LONG_STAY -> cents(subtotal.multiply(LONG_STAY_DISCOUNT));
            case WEEKEND_PACKAGE -> cents(BigDecimal.ZERO);
        };
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));""")

SERVICE = RESERVATIONS + "ReservationService.java"
naive.replace(SERVICE, "import java.math.BigDecimal;\n",
        "import java.math.BigDecimal;\nimport java.time.DayOfWeek;\nimport java.time.temporal.ChronoUnit;\n")
naive.replace(SERVICE, """        Price price = priceCalculator.calculate(room, request.checkIn(), request.checkOut(), request.ratePlan());""",
        """        if (request.ratePlan() == RatePlan.CORPORATE
                && (request.companyCode() == null
                    || !PriceCalculator.COMPANY_DISCOUNTS.containsKey(request.companyCode()))) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "the CORPORATE plan needs the code of a company with an agreement");
        }
        if (request.ratePlan() == RatePlan.WEEKEND_PACKAGE
                && (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || !request.checkOut().equals(request.checkIn().plusDays(2)))) {
            throw new HotelException(ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from a Friday to the Sunday right after");
        }
        if (request.ratePlan() == RatePlan.LONG_STAY
                && ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < 7) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay needs at least 7 nights");
        }
        Price price = priceCalculator.calculate(
                room, request.checkIn(), request.checkOut(), request.ratePlan(), request.companyCode());""")
naive.replace(SERVICE, """        BigDecimal refund = switch (reservation.ratePlan()) {
            case STANDARD -> reservation.amountPaid();
            case NON_REFUNDABLE -> PriceCalculator.cents(BigDecimal.ZERO);
        };""", """        BigDecimal refund = switch (reservation.ratePlan()) {
            case STANDARD, CORPORATE, LONG_STAY -> reservation.amountPaid();
            case NON_REFUNDABLE -> PriceCalculator.cents(BigDecimal.ZERO);
            case WEEKEND_PACKAGE -> PriceCalculator.cents(reservation.amountPaid().multiply(new BigDecimal("0.5")));
        };""")

REQUEST = "src/main/java/hotel/web/CreateReservationRequest.java"
naive.replace(REQUEST, """        Integer guests,
        RatePlan ratePlan) {""", """        Integer guests,
        RatePlan ratePlan,
        String companyCode) {""")
naive.replace("src/main/java/hotel/web/HttpApi.java", """        return new NewReservation(
                body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(), body.ratePlan());""",
        """        return new NewReservation(body.roomNumber(), guest, body.checkIn(), body.checkOut(), body.guests(),
                body.ratePlan(), body.companyCode());""")

naive.replace("src/test/java/hotel/reservations/ReservationServiceTest.java",
        """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);""",
        """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan, null);""")
naive.replace("src/test/java/hotel/reports/ReportsTest.java",
        """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan));""",
        """                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan, null));""")

PRICE_TEST = "src/test/java/hotel/reservations/PriceCalculatorTest.java"
naive.replace(PRICE_TEST, "RatePlan.STANDARD);", "RatePlan.STANDARD, null);", count=3)
naive.replace(PRICE_TEST, "RatePlan.NON_REFUNDABLE);", "RatePlan.NON_REFUNDABLE, null);", count=2)

naive.build_and_save()
