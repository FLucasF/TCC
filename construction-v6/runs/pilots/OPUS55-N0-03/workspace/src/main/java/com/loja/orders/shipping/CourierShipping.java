package com.loja.orders.shipping;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.common.Money;

/** Motoboy: preço fixo, entrega no mesmo dia, só até 5 kg. */
@Component
public class CourierShipping implements ShippingOption {

    private static final BigDecimal PRICE = Money.of("18.00");
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5");

    @Override
    public String code() {
        return "COURIER";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        if (request.weightKg().compareTo(MAX_WEIGHT_KG) > 0) {
            throw ApiException.unprocessable(ErrorCode.SHIPPING_METHOD_UNAVAILABLE);
        }
        return new ShippingQuote(PRICE, 0);
    }

    @Override
    public boolean coveredByClubFreeShipping() {
        return false;
    }
}
