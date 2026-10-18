package br.com.loja.shipping;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.common.Money;

@Component
class CourierShipping implements ShippingOption {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5");

    @Override
    public String code() {
        return "COURIER";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        if (request.weightKg().compareTo(MAX_WEIGHT_KG) > 0) {
            throw new ApiException(ErrorCode.SHIPPING_METHOD_UNAVAILABLE);
        }
        return new ShippingQuote(Money.of("18.00"), 0);
    }

    @Override
    public boolean freightWaivableByClub() {
        return false;
    }
}
