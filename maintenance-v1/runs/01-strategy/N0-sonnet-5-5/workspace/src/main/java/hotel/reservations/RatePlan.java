package hotel.reservations;

import static hotel.reservations.PriceCalculator.cents;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * A rate plan owns its rules: whether a stay may use it, how the stay is priced, and how much comes back
 * when a paid reservation is cancelled.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price base) {
            return base;
        }
    },
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price base) {
            return discounted(base, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },
    /** A discount agreed with a company, given by the company code; everything comes back on cancellation. */
    CORPORATE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price base) {
            BigDecimal rate = CorporateAgreements.discountFor(companyCode)
                    .orElseThrow(() -> new HotelException(
                            ErrorCode.INVALID_REQUEST, "the corporate plan needs the code of a company with an agreement"));
            return discounted(base, rate);
        }
    },
    /** Friday to Sunday, exactly two nights, at a closed price all year; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price base) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(2))) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the following Sunday");
            }
            BigDecimal packagePrice = cents(switch (room.type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            });
            return new Price(base.nights(), packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },
    /** 10% off for stays of 7 nights or more; everything comes back on cancellation. */
    LONG_STAY {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price base) {
            if (base.nights() < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay plan needs 7 nights or more");
            }
            return discounted(base, new BigDecimal("0.10"));
        }
    };

    /**
     * The final price of a stay, given its price without any discount. Throws INVALID_REQUEST when the
     * stay cannot use this plan.
     */
    abstract Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price base);

    /** What comes back when a reservation that has paid {@code amountPaid} is cancelled. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static Price discounted(Price base, BigDecimal rate) {
        BigDecimal discount = cents(base.subtotal().multiply(rate));
        return new Price(base.nights(), base.subtotal(), discount, base.subtotal().subtract(discount));
    }
}
