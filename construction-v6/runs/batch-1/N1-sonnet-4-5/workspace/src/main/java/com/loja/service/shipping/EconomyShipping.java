package com.loja.service.shipping;

import com.loja.domain.ShippingMethod;
import com.loja.integration.TransLogClient;
import org.springframework.stereotype.Component;

@Component
public class EconomyShipping extends TransLogShipping {

    public EconomyShipping(TransLogClient transLogClient) {
        super(transLogClient);
    }

    @Override
    public ShippingMethod getMethod() {
        return ShippingMethod.ECONOMY;
    }

    @Override
    protected String getServiceCode() {
        return "STD";
    }
}
