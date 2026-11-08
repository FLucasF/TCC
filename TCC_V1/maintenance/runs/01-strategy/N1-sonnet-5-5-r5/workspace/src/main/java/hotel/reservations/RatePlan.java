package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;

/**
 * A rate plan: who may use it, how the price is made from the nights, and what comes back when a
 * reservation is cancelled. A new plan is a new constant that overrides only what is different.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        BigDecimal discount(BigDecimal subtotal, NewReservation request) {
            return percentage(subtotal, "0.15");
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },
    /** Company agreement: the discount depends on the company code, which is required. */
    CORPORATE {
        @Override
        void checkAllowed(NewReservation request) {
            discountOf(request);
        }

        @Override
        BigDecimal discount(BigDecimal subtotal, NewReservation request) {
            return PriceCalculator.cents(subtotal.multiply(discountOf(request)));
        }

        private BigDecimal discountOf(NewReservation request) {
            return CorporateAgreements.discountFor(request.companyCode())
                    .orElseThrow(() -> invalid("the corporate rate plan needs the code of a company with an agreement"));
        }
    },
    /** Friday to Sunday, a fixed price per room type all year round; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void checkAllowed(NewReservation request) {
            if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) != 2) {
                throw invalid("the weekend package is from a Friday to the Sunday after it");
            }
        }

        @Override
        BigDecimal subtotal(Room room, BigDecimal nightsTotal) {
            return PriceCalculator.cents(switch (room.type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            });
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return percentage(amountPaid, "0.50");
        }
    },
    /** 7 nights or more, 10% off. */
    LONG_STAY {
        @Override
        void checkAllowed(NewReservation request) {
            if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < 7) {
                throw invalid("the long stay rate plan needs at least 7 nights");
            }
        }

        @Override
        BigDecimal discount(BigDecimal subtotal, NewReservation request) {
            return percentage(subtotal, "0.10");
        }
    };

    /** Refuses, with INVALID_REQUEST, a reservation that this plan does not allow. */
    void checkAllowed(NewReservation request) {
    }

    /** The price before the discount, given what the nights alone add up to. */
    BigDecimal subtotal(Room room, BigDecimal nightsTotal) {
        return nightsTotal;
    }

    BigDecimal discount(BigDecimal subtotal, NewReservation request) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }

    /** What goes back to the guest who cancels after paying that amount. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static BigDecimal percentage(BigDecimal amount, String rate) {
        return PriceCalculator.cents(amount.multiply(new BigDecimal(rate)));
    }

    private static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }
}
