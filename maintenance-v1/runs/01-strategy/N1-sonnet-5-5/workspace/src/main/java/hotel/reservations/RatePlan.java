package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * A way of selling a stay. Each plan says who can use it, how the nights are priced and what comes back
 * when the guest cancels.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Stay stay, Price nightsPrice) {
            return discounted(nightsPrice, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },
    /** For guests of companies with an agreement: the company's discount; everything comes back. */
    CORPORATE {
        @Override
        void checkEligibility(Stay stay) {
            CorporateAgreement.forCode(stay.companyCode());
        }

        @Override
        Price price(Stay stay, Price nightsPrice) {
            return discounted(nightsPrice, CorporateAgreement.forCode(stay.companyCode()).discount());
        }
    },
    /** Friday to Sunday, two nights, at a closed price for the room type; half comes back. */
    WEEKEND_PACKAGE {
        private final Map<RoomType, BigDecimal> prices = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        void checkEligibility(Stay stay) {
            if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the WEEKEND_PACKAGE plan is from Friday to Sunday, two nights");
            }
        }

        @Override
        Price price(Stay stay, Price nightsPrice) {
            BigDecimal closedPrice = prices.get(stay.room().type());
            return new Price(nightsPrice.nights(), closedPrice, PriceCalculator.cents(BigDecimal.ZERO), closedPrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.50")));
        }
    },
    /** For stays of 7 nights or more: 10% off; everything comes back. */
    LONG_STAY {
        @Override
        void checkEligibility(Stay stay) {
            if (stay.nights() < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the LONG_STAY plan needs 7 nights or more");
            }
        }

        @Override
        Price price(Stay stay, Price nightsPrice) {
            return discounted(nightsPrice, new BigDecimal("0.10"));
        }
    };

    /** Throws INVALID_REQUEST if the stay cannot use this plan. */
    void checkEligibility(Stay stay) {
    }

    /** The price of the stay under this plan, given the price of its nights (high season included). */
    Price price(Stay stay, Price nightsPrice) {
        return nightsPrice;
    }

    /** What goes back to the guest who paid {@code amountPaid} and cancels. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static Price discounted(Price nightsPrice, BigDecimal rate) {
        BigDecimal discount = PriceCalculator.cents(nightsPrice.subtotal().multiply(rate));
        return new Price(nightsPrice.nights(), nightsPrice.subtotal(), discount, nightsPrice.subtotal().subtract(discount));
    }
}
