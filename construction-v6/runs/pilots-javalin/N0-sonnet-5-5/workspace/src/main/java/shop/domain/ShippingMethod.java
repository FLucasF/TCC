package shop.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public interface ShippingMethod {
    record Request(String zipCode, BigDecimal weightKg, Region region) {
        public int weightGrams() {
            return weightKg.multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.HALF_UP).intValueExact();
        }
    }

    record Quote(BigDecimal cost, int days) {
    }

    String code();

    /** Refuses with SHIPPING_METHOD_UNAVAILABLE or CARRIER_UNAVAILABLE when it cannot serve the order. */
    Quote quote(Request request);

    /** False when there is no freight at all (e.g. store pickup). */
    default boolean chargesFreight() {
        return true;
    }

    /** False when the tier's free-shipping perk does not cover this method (third-party courier). */
    default boolean coveredByTierFreeShipping() {
        return true;
    }

    /** Fixed price and lead time. */
    record Fixed(String code, BigDecimal cost, int days, boolean chargesFreight, boolean coveredByTierFreeShipping,
                 BigDecimal maxWeightKg) implements ShippingMethod {
        @Override
        public Quote quote(Request request) {
            if (maxWeightKg != null && request.weightKg().compareTo(maxWeightKg) > 0) {
                throw ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
            }
            return new Quote(cost, days);
        }
    }

    /** Priced by an external carrier; the distribution center adds days in some regions. */
    record Carrier(String code, CarrierGateway gateway, String service) implements ShippingMethod {
        @Override
        public Quote quote(Request request) {
            CarrierGateway.Quote q;
            try {
                q = gateway.quote(request.zipCode(), request.weightGrams(), service)
                        .orElseThrow(() -> ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE"));
            } catch (CarrierGateway.UnavailableException e) {
                throw new ApiException("CARRIER_UNAVAILABLE", 503);
            }
            return new Quote(q.cost(), q.days() + request.region().extraDispatchDays());
        }
    }
}
