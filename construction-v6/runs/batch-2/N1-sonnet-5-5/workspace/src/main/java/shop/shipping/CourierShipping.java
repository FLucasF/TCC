package shop.shipping;

import shop.ApiException;
import shop.Money;

import java.math.BigDecimal;

public final class CourierShipping implements ShippingOption {
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5");

    @Override
    public String code() {
        return "COURIER";
    }

    @Override
    public ShippingQuote quote(ShippingContext context) {
        if (context.weightKg().compareTo(MAX_WEIGHT_KG) > 0) {
            throw ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
        }
        return new ShippingQuote(Money.of("18.00"), 0);
    }

    @Override
    public boolean acceptsClubFreeShipping() {
        return false;
    }
}
