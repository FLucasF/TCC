package com.loja.service.shipping;

import com.loja.domain.ShippingMethod;
import com.loja.integration.TransLogClient;
import org.springframework.stereotype.Component;

@Component
public class ExpressShipping extends TransLogShipping {

    public ExpressShipping(TransLogClient transLogClient) {
        super(transLogClient);
    }

    @Override
    public ShippingMethod getMethod() {
        return ShippingMethod.EXPRESS;
    }

    @Override
    protected String getServiceCode() {
        return "EXP";
    }
}
