package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * A way of selling a stay. Each plan says how the stay is priced, who may use it and how much of what
 * was paid comes back when the guest cancels; the defaults are those of the standard plan.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(NewReservation request, Room room, Price seasonal) {
            return seasonal.withDiscount(PriceCalculator.cents(seasonal.subtotal().multiply(new BigDecimal("0.15"))));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },
    /** A discount that depends on the company informed in the reservation; everything comes back. */
    CORPORATE {
        @Override
        void validate(NewReservation request) {
            if (Company.find(request.companyCode()).isEmpty()) {
                throw invalid("the corporate plan needs the code of a company with an agreement");
            }
        }

        @Override
        Price price(NewReservation request, Room room, Price seasonal) {
            Company company = Company.find(request.companyCode()).orElseThrow();
            return seasonal.withDiscount(PriceCalculator.cents(seasonal.subtotal().multiply(company.discount())));
        }
    },
    /** Friday to Sunday, two nights, for a closed price all year round; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void validate(NewReservation request) {
            if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || !request.checkOut().equals(request.checkIn().plusDays(2))) {
                throw invalid("the weekend package is from a Friday to the Sunday after it");
            }
        }

        @Override
        Price price(NewReservation request, Room room, Price seasonal) {
            BigDecimal closedPrice = PriceCalculator.cents(new BigDecimal(switch (room.type()) {
                case STANDARD -> "520.00";
                case DELUXE -> "820.00";
                case SUITE -> "1200.00";
            }));
            return new Price(seasonal.nights(), closedPrice, PriceCalculator.cents(BigDecimal.ZERO), closedPrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },
    /** 10% off for 7 nights or more, on top of the high-season prices; everything comes back. */
    LONG_STAY {
        @Override
        void validate(NewReservation request) {
            if (request.checkIn().plusDays(7).isAfter(request.checkOut())) {
                throw invalid("the long stay plan needs at least 7 nights");
            }
        }

        @Override
        Price price(NewReservation request, Room room, Price seasonal) {
            return seasonal.withDiscount(PriceCalculator.cents(seasonal.subtotal().multiply(new BigDecimal("0.10"))));
        }
    };

    /** Throws INVALID_REQUEST when the plan cannot be used for this request. */
    void validate(NewReservation request) {
    }

    /** The price of the stay, given what it costs night by night (with no discount yet). */
    Price price(NewReservation request, Room room, Price seasonal) {
        return seasonal;
    }

    /** What goes back to the guest who cancels, given what was paid. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }
}
