package br.com.loja.pedidos.shipping;

import br.com.loja.pedidos.common.ApiException;
import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CourierShipping implements ShippingOption {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5");

    @Override
    public String code() {
        return "COURIER";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        if (request.weightKg().compareTo(MAX_WEIGHT_KG) > 0) {
            throw ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
        }
        return new ShippingQuote(Money.of("18.00"), 0);
    }

    /** The courier is outsourced, so club shipping perks do not cover it. */
    @Override
    public boolean clubFreeShippingApplies() {
        return false;
    }
}
