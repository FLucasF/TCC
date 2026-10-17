package hotel.reservations;

import static hotel.reservations.PriceCalculator.cents;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * A rate plan: how it prices a stay, who is allowed to book it, and what comes back when it is
 * cancelled. Each plan is one constant that answers these three questions on its own, so the next
 * plan the commercial team invents is a new constant here and nothing else has to change.
 */
public enum RatePlan {

    /** Full price; everything comes back on cancellation. */
    STANDARD {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.priceOfNights(room, checkIn, checkOut, NO_DISCOUNT);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off the nights; nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.priceOfNights(room, checkIn, checkOut, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },

    /** Company deal: a per-company discount on the nights; everything comes back on cancellation. */
    CORPORATE {
        @Override
        void validate(NewReservation request) {
            if (request.companyCode() == null || !COMPANY_DISCOUNTS.containsKey(request.companyCode())) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the corporate rate needs a known company code");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.priceOfNights(room, checkIn, checkOut, COMPANY_DISCOUNTS.get(companyCode));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** Fixed-price Friday-to-Sunday package; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void validate(NewReservation request) {
            boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                    && request.checkOut().equals(request.checkIn().plusDays(2));
            if (!fridayToSunday) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package runs from a Friday to the next Sunday");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            BigDecimal packagePrice = cents(WEEKEND_PRICES.get(room.type()));
            int nights = calculator.countNights(checkIn, checkOut);
            return new Price(nights, packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** Seven nights or more, 10% off the nights; everything comes back on cancellation. */
    LONG_STAY {
        @Override
        void validate(NewReservation request) {
            if (request.checkIn().plusDays(MIN_LONG_STAY_NIGHTS).isAfter(request.checkOut())) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the long-stay rate needs at least " + MIN_LONG_STAY_NIGHTS + " nights");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.priceOfNights(room, checkIn, checkOut, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    private static final BigDecimal NO_DISCOUNT = BigDecimal.ZERO;
    private static final int MIN_LONG_STAY_NIGHTS = 7;

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private static final Map<RoomType, BigDecimal> WEEKEND_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    /** Whether this plan may be booked for the request; open to anyone unless the plan says otherwise. */
    void validate(NewReservation request) {}

    /** The price of the stay under this plan. */
    abstract Price price(
            PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** What comes back to the guest who cancels after paying {@code amountPaid}. */
    abstract BigDecimal refund(BigDecimal amountPaid);
}
