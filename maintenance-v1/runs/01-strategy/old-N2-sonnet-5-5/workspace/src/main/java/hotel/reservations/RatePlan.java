package hotel.reservations;

import static hotel.reservations.PriceCalculator.cents;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * A rate plan decides who may use it, how the stay is priced and what comes back on cancellation. A new
 * plan is a new constant here.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Room room, NewReservation request, Price regular) {
            return discounted(regular, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },
    /** Company agreement: the company's discount, informed by its code; everything comes back on cancellation. */
    CORPORATE {
        @Override
        void validate(NewReservation request, Price regular) {
            if (Company.withCode(request.companyCode()).isEmpty()) {
                throw invalid("the corporate rate needs the code of a company with an agreement");
            }
        }

        @Override
        Price price(Room room, NewReservation request, Price regular) {
            Company company = Company.withCode(request.companyCode()).orElseThrow();
            return discounted(regular, company.discount());
        }
    },
    /** Friday to Sunday, two nights, for a closed price all year; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void validate(NewReservation request, Price regular) {
            if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || !request.checkOut().equals(request.checkIn().plusDays(2))) {
                throw invalid("the weekend package is from a Friday to the Sunday after it");
            }
        }

        @Override
        Price price(Room room, NewReservation request, Price regular) {
            BigDecimal packagePrice = switch (room.type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            };
            return new Price(regular.nights(), packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },
    /** 7 nights or more, 10% off; everything comes back on cancellation. */
    LONG_STAY {
        @Override
        void validate(NewReservation request, Price regular) {
            if (regular.nights() < 7) {
                throw invalid("the long stay rate needs at least 7 nights");
            }
        }

        @Override
        Price price(Room room, NewReservation request, Price regular) {
            return discounted(regular, new BigDecimal("0.10"));
        }
    };

    /** Refuses the reservation (INVALID_REQUEST) when this plan cannot be used for it. */
    void validate(NewReservation request, Price regular) {
    }

    /** The price of the stay under this plan, given its price at full rate. */
    Price price(Room room, NewReservation request, Price regular) {
        return regular;
    }

    /** What comes back to the guest who paid {@code amountPaid} and cancels. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static Price discounted(Price regular, BigDecimal rate) {
        BigDecimal discount = cents(regular.subtotal().multiply(rate));
        return new Price(regular.nights(), regular.subtotal(), discount, regular.subtotal().subtract(discount));
    }

    private static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }
}
