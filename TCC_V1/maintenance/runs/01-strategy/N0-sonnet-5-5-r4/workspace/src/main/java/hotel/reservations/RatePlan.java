package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * A way of selling a stay. Each plan knows how it prices a stay, who can book it (by throwing an
 * INVALID_REQUEST {@link HotelException}), and how much of the payment comes back on cancellation.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.nightlyPrice(room, checkIn, checkOut, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return calculator.nightlyPrice(room, checkIn, checkOut, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },
    /** Company agreement: the company's discount, which needs a valid company code. Full refund. */
    CORPORATE {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            BigDecimal discount = CorporateAgreements.discountFor(companyCode)
                    .orElseThrow(() -> invalid("the corporate plan needs the code of a company with an agreement"));
            return calculator.nightlyPrice(room, checkIn, checkOut, discount);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },
    /** Friday to Sunday (two nights) for a closed price all year round. Half comes back on cancellation. */
    WEEKEND_PACKAGE {
        private final Map<RoomType, BigDecimal> prices = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || ChronoUnit.DAYS.between(checkIn, checkOut) != 2) {
                throw invalid("the weekend package is from a Friday to the following Sunday");
            }
            BigDecimal packagePrice = prices.get(room.type());
            return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },
    /** 7 nights or more: 10% off. Full refund. */
    LONG_STAY {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (ChronoUnit.DAYS.between(checkIn, checkOut) < 7) {
                throw invalid("the long stay plan needs at least 7 nights");
            }
            return calculator.nightlyPrice(room, checkIn, checkOut, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    /** The price of the stay under this plan; throws INVALID_REQUEST when the plan cannot be used for it. */
    abstract Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** What goes back to the guest, given what they paid, when the reservation is cancelled. */
    abstract BigDecimal refund(BigDecimal amountPaid);

    private static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }
}
