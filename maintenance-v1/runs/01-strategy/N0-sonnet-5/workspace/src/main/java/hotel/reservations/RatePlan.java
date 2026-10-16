package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * Each rate plan has its own way of pricing a stay, its own rule for who may book it, and its own
 * refund rule on cancellation.
 */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            PriceCalculator.NightlyTotal nightly = calculator.sumNights(room, checkIn, checkOut);
            return new Price(nightly.nights(), nightly.subtotal(), PriceCalculator.cents(BigDecimal.ZERO), nightly.subtotal());
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            return discounted(calculator.sumNights(room, checkIn, checkOut), new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /**
     * Corporate agreement: a discount that depends on the company code the guest informs. Full
     * refund on cancellation, like {@link #STANDARD}.
     */
    CORPORATE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            BigDecimal discountRate = companyCode == null ? null : COMPANY_DISCOUNTS.get(companyCode);
            if (discountRate == null) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the CORPORATE rate plan needs a valid company code");
            }
            return discounted(calculator.sumNights(room, checkIn, checkOut), discountRate);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /**
     * Friday check-in, Sunday check-out, exactly two nights, at a flat price that holds all year,
     * season or not. Half of what was paid comes back on cancellation.
     */
    WEEKEND_PACKAGE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.isEqual(checkIn.plusDays(2))) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the WEEKEND_PACKAGE rate plan only covers Friday check-in to Sunday check-out");
            }
            BigDecimal flatPrice = WEEKEND_PACKAGE_PRICES.get(room.type());
            return new Price(2, flatPrice, PriceCalculator.cents(BigDecimal.ZERO), flatPrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** 7 nights or more, 10% off; everything comes back on cancellation. */
    LONG_STAY {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            PriceCalculator.NightlyTotal nightly = calculator.sumNights(room, checkIn, checkOut);
            if (nightly.nights() < 7) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the LONG_STAY rate plan needs at least 7 nights");
            }
            return discounted(nightly, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private static final Map<RoomType, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    /** The price of the stay under this plan; throws {@code INVALID_REQUEST} if the plan does not apply. */
    abstract Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator);

    /** What comes back when a reservation under this plan is cancelled, given what was paid. */
    abstract BigDecimal refund(BigDecimal amountPaid);

    private static Price discounted(PriceCalculator.NightlyTotal nightly, BigDecimal discountRate) {
        BigDecimal discount = PriceCalculator.cents(nightly.subtotal().multiply(discountRate));
        return new Price(nightly.nights(), nightly.subtotal(), discount, nightly.subtotal().subtract(discount));
    }
}
