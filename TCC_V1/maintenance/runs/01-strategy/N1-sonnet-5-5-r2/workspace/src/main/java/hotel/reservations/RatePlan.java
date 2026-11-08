package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * A way of selling a stay. Each plan says who can use it ({@link #validate}), how the stay is priced
 * ({@link #price}) and what comes back when the guest cancels ({@link #refund}). A new plan is a new
 * constant here.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Room room, NewReservation request, Price nightsPrice) {
            return withDiscount(nightsPrice, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** The discount of the guest's company (given by its code); without a known company, no reservation. */
    CORPORATE {
        @Override
        void validate(Room room, NewReservation request) {
            company(request);
        }

        @Override
        Price price(Room room, NewReservation request, Price nightsPrice) {
            return withDiscount(nightsPrice, company(request).discount());
        }

        private Company company(NewReservation request) {
            return Company.fromCode(request.companyCode())
                    .orElseThrow(() -> new HotelException(
                            ErrorCode.INVALID_REQUEST, "the corporate plan needs the code of a company with an agreement"));
        }
    },

    /** Friday to Sunday, exactly two nights, for a fixed price per room type all year round; half comes back. */
    WEEKEND_PACKAGE {
        private final Map<RoomType, BigDecimal> prices = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        void validate(Room room, NewReservation request) {
            if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || !request.checkOut().equals(request.checkIn().plusDays(2))) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the Sunday after it");
            }
        }

        @Override
        Price price(Room room, NewReservation request, Price nightsPrice) {
            BigDecimal packagePrice = prices.get(room.type());
            return new Price(nightsPrice.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** 10% off for 7 nights or more. */
    LONG_STAY {
        private static final int MINIMUM_NIGHTS = 7;

        @Override
        void validate(Room room, NewReservation request) {
            if (request.checkIn().plusDays(MINIMUM_NIGHTS).isAfter(request.checkOut())) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the long stay plan needs " + MINIMUM_NIGHTS + " nights or more");
            }
        }

        @Override
        Price price(Room room, NewReservation request, Price nightsPrice) {
            return withDiscount(nightsPrice, new BigDecimal("0.10"));
        }
    };

    /** Throws INVALID_REQUEST when the reservation cannot use this plan. */
    void validate(Room room, NewReservation request) {
    }

    /** The price of the stay; {@code nightsPrice} is the sum of the nights, with no discount. */
    Price price(Room room, NewReservation request, Price nightsPrice) {
        return nightsPrice;
    }

    /** What goes back to the guest who paid {@code amountPaid} and cancels. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static Price withDiscount(Price nightsPrice, BigDecimal rate) {
        BigDecimal discount = PriceCalculator.cents(nightsPrice.subtotal().multiply(rate));
        return new Price(nightsPrice.nights(), nightsPrice.subtotal(), discount, nightsPrice.subtotal().subtract(discount));
    }
}
