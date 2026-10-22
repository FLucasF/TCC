package hotel.reservations;

import static hotel.reservations.PriceCalculator.cents;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * How a reservation is priced, who may book it and what comes back when it is cancelled. A new plan is a
 * new constant that overrides only what differs from the default: the nights at full price, open to
 * everyone, everything paid comes back.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Stay stay) {
            return discounted(stay, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },
    /** The discount of the guest's company, given by its code; everything comes back on cancellation. */
    CORPORATE {
        @Override
        void check(Stay stay) {
            if (Company.withCode(stay.companyCode()).isEmpty()) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the corporate rate needs the code of a company with an agreement");
            }
        }

        @Override
        Price price(Stay stay) {
            return discounted(stay, Company.withCode(stay.companyCode()).orElseThrow().discount());
        }
    },
    /** Friday to Sunday, two nights, for a fixed price all year round; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void check(Stay stay) {
            if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the Sunday after");
            }
        }

        @Override
        Price price(Stay stay) {
            BigDecimal packagePrice = switch (stay.room().type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            };
            return new Price(stay.nights(), packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.50")));
        }
    },
    /** 10% off for 7 nights or more; everything comes back on cancellation. */
    LONG_STAY {
        @Override
        void check(Stay stay) {
            if (stay.nights() < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay needs at least 7 nights");
            }
        }

        @Override
        Price price(Stay stay) {
            return discounted(stay, new BigDecimal("0.10"));
        }
    };

    /** Refuses, with an INVALID_REQUEST, a stay this plan does not allow. */
    void check(Stay stay) {
    }

    /** The price of an accepted stay. */
    Price price(Stay stay) {
        return discounted(stay, BigDecimal.ZERO);
    }

    /** What goes back to the guest who cancels after paying {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    static Price discounted(Stay stay, BigDecimal rate) {
        BigDecimal discount = cents(stay.nightsTotal().multiply(rate));
        return new Price(stay.nights(), stay.nightsTotal(), discount, stay.nightsTotal().subtract(discount));
    }
}
