package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A rate plan says who may book under it, how its price is calculated, and how much comes back when
 * the guest cancels before the stay starts. The business keeps adding plans, each with its own rules
 * for those three things, so a new plan is a new constant here and nowhere else.
 */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {}

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.nightlyPrice(room, checkIn, checkOut, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(Price price, BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {}

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.nightlyPrice(room, checkIn, checkOut, PriceCalculator.NON_REFUNDABLE_DISCOUNT);
        }

        @Override
        BigDecimal refund(Price price, BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /**
     * A company's negotiated discount. The guest informs the company code when booking; cancelling
     * gives everything back, like the standard plan.
     */
    CORPORATE {
        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (CorporateAgreement.byCode(companyCode).isEmpty()) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "'" + companyCode + "' is not a company with a corporate rate");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            BigDecimal discount = CorporateAgreement.byCode(companyCode).orElseThrow().discount();
            return calculator.nightlyPrice(room, checkIn, checkOut, discount);
        }

        @Override
        BigDecimal refund(Price price, BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /**
     * Friday to Sunday, exactly two nights, for a closed price that holds all year. Half of what was
     * paid comes back on cancellation.
     */
    WEEKEND_PACKAGE {
        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(2))) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package only runs from Friday to Sunday");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            BigDecimal packagePrice = PriceCalculator.cents(weekendPackagePrice(room));
            BigDecimal noDiscount = PriceCalculator.cents(BigDecimal.ZERO);
            return new Price(2, packagePrice, noDiscount, packagePrice);
        }

        @Override
        BigDecimal refund(Price price, BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }

        private BigDecimal weekendPackagePrice(Room room) {
            return switch (room.type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            };
        }
    },

    /** 7 nights or more get 10% off; high-season nights are still priced at their surcharge. */
    LONG_STAY {
        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (ChronoUnit.DAYS.between(checkIn, checkOut) < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay plan needs at least 7 nights");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.nightlyPrice(room, checkIn, checkOut, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(Price price, BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    /** Whether a stay may be booked under this plan at all; throws INVALID_REQUEST when it cannot. */
    abstract void validate(LocalDate checkIn, LocalDate checkOut, String companyCode);

    abstract Price price(
            PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** What comes back to the guest on cancellation, given what the reservation was priced at and paid. */
    abstract BigDecimal refund(Price price, BigDecimal amountPaid);
}
