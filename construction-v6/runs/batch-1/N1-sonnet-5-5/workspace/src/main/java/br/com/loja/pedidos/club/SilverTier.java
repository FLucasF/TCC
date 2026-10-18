package br.com.loja.pedidos.club;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.Money;

@Component
public class SilverTier implements ClubTier {

    private static final BigDecimal CREDIT_RATE = new BigDecimal("0.02");

    @Override
    public String code() {
        return "SILVER";
    }

    @Override
    public BigDecimal nextPurchaseCredit(BigDecimal productsSubtotal) {
        return Money.percent(productsSubtotal, CREDIT_RATE);
    }
}
