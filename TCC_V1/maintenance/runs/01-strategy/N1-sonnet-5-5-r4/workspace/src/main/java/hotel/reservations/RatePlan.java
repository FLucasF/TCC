package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;

/**
 * A rate plan: who can use it, how the stay is priced and what comes back when the guest cancels. A new
 * plan is a new constant with its own rules.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        Price price(Room room, NewReservation request, int nights, BigDecimal subtotal) {
            return discounted(nights, subtotal, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Room room, NewReservation request, int nights, BigDecimal subtotal) {
            return discounted(nights, subtotal, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },
    /** The discount agreed with the guest's company, who must inform its code; everything comes back on cancellation. */
    CORPORATE {
        @Override
        void validate(NewReservation request) {
            company(request);
        }

        @Override
        Price price(Room room, NewReservation request, int nights, BigDecimal subtotal) {
            return discounted(nights, subtotal, company(request).discount());
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }

        private Company company(NewReservation request) {
            return Company.withCode(request.companyCode())
                    .orElseThrow(() -> new HotelException(
                            ErrorCode.INVALID_REQUEST, "the corporate plan needs the code of a company with an agreement"));
        }
    },
    /** Friday to Sunday, two nights, at a fixed price all year round; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void validate(NewReservation request) {
            if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || !request.checkOut().equals(request.checkIn().plusDays(2))) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the Sunday after it");
            }
        }

        @Override
        Price price(Room room, NewReservation request, int nights, BigDecimal subtotal) {
            BigDecimal packagePrice = PriceCalculator.cents(switch (room.type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            });
            return discounted(nights, packagePrice, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.50")));
        }
    },
    /** 7 nights or more, with 10% off; everything comes back on cancellation. */
    LONG_STAY {
        private static final int MINIMUM_NIGHTS = 7;

        @Override
        void validate(NewReservation request) {
            if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < MINIMUM_NIGHTS) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the long stay plan needs at least " + MINIMUM_NIGHTS + " nights");
            }
        }

        @Override
        Price price(Room room, NewReservation request, int nights, BigDecimal subtotal) {
            return discounted(nights, subtotal, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    /** Throws INVALID_REQUEST when the reservation cannot use this plan. */
    void validate(NewReservation request) {
    }

    /** The price of the stay; {@code subtotal} is the sum of the nights at the room's rates, season included. */
    abstract Price price(Room room, NewReservation request, int nights, BigDecimal subtotal);

    /** What goes back to the guest who paid {@code amountPaid} and cancels. */
    abstract BigDecimal refund(BigDecimal amountPaid);

    static Price discounted(int nights, BigDecimal subtotal, BigDecimal discountRate) {
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }
}
