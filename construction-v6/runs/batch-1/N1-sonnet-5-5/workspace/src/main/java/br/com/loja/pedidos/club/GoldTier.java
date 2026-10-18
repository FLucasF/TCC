package br.com.loja.pedidos.club;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.Money;
import br.com.loja.pedidos.shipping.ShippingOption;

@Component
public class GoldTier implements ClubTier {

    private static final BigDecimal CREDIT_RATE = new BigDecimal("0.05");
    private static final BigDecimal GIFT_THRESHOLD = new BigDecimal("500.00");

    @Override
    public String code() {
        return "GOLD";
    }

    @Override
    public BigDecimal nextPurchaseCredit(BigDecimal productsSubtotal) {
        return Money.percent(productsSubtotal, CREDIT_RATE);
    }

    @Override
    public boolean freeShipping(ShippingOption shipping) {
        return shipping.clubFreeShippingApplies();
    }

    @Override
    public boolean gift(BigDecimal productsSubtotal) {
        return productsSubtotal.compareTo(GIFT_THRESHOLD) > 0;
    }
}
