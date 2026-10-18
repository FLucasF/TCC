package com.loja.service.shipping;

import com.loja.domain.Money;
import com.loja.domain.Region;
import com.loja.domain.ShippingMethod;
import com.loja.integration.TransLogClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class TransLogShipping implements ShippingStrategy {

    private final TransLogClient transLogClient;

    protected TransLogShipping(TransLogClient transLogClient) {
        this.transLogClient = transLogClient;
    }

    protected abstract String getServiceCode();

    @Override
    public boolean isAvailable(ShippingContext context) {
        try {
            TransLogClient.QuoteResult result = transLogClient.getQuote(
                context.zipCode(),
                context.totalWeight().toGrams(),
                getServiceCode()
            );
            return result != null && result.priceCents() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ShippingResult calculateShipping(ShippingContext context) {
        TransLogClient.QuoteResult result = transLogClient.getQuote(
            context.zipCode(),
            context.totalWeight().toGrams(),
            getServiceCode()
        );

        Money cost = new Money(BigDecimal.valueOf(result.priceCents()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN));
        int baseDays = (int) Math.ceil(result.etaHours() / 24.0);
        int extraDays = getExtraDays(context.region());

        return new ShippingResult(cost, baseDays + extraDays);
    }

    private int getExtraDays(Region region) {
        return switch (region) {
            case NORTH -> 3;
            case NORTHEAST -> 2;
            default -> 0;
        };
    }
}
