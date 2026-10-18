package shop.domain.shipping;

import java.math.BigDecimal;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.domain.Money;

public class Courier implements ShippingOption {
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5");
    private static final BigDecimal COST = Money.of("18.00");

    @Override
    public String code() {
        return "COURIER";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        if (request.weightKg().compareTo(MAX_WEIGHT_KG) > 0) {
            throw new DomainException(ErrorCode.SHIPPING_METHOD_UNAVAILABLE);
        }
        return new ShippingQuote(COST, 0);
    }

    @Override
    public boolean chargesFreight() {
        return true;
    }

    /** Motoboy é terceirizado: o clube não cobre o frete. */
    @Override
    public boolean clubWaivable() {
        return false;
    }
}
