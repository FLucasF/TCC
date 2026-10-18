package br.com.loja.pedidos.shipping;

import br.com.loja.pedidos.common.Money;
import org.springframework.stereotype.Component;

@Component
public class StorePickupShipping implements ShippingOption {

    @Override
    public String code() {
        return "STORE_PICKUP";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        return new ShippingQuote(Money.ZERO, 1);
    }
}
