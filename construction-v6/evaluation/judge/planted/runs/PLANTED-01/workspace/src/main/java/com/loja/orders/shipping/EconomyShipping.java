package com.loja.orders.shipping;

import org.springframework.stereotype.Component;

import com.loja.orders.shipping.carrier.Carrier;
import com.loja.orders.shipping.carrier.CarrierService;

@Component
public class EconomyShipping extends CarrierShippingOption {

    public EconomyShipping(Carrier carrier) {
        super(carrier, CarrierService.STANDARD);
    }

    @Override
    public String code() {
        return "ECONOMY";
    }
}
