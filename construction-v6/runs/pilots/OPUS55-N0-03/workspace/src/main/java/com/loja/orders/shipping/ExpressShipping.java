package com.loja.orders.shipping;

import org.springframework.stereotype.Component;

import com.loja.orders.shipping.carrier.Carrier;
import com.loja.orders.shipping.carrier.CarrierService;

@Component
public class ExpressShipping extends CarrierShippingOption {

    public ExpressShipping(Carrier carrier) {
        super(carrier, CarrierService.EXPRESS);
    }

    @Override
    public String code() {
        return "EXPRESS";
    }
}
