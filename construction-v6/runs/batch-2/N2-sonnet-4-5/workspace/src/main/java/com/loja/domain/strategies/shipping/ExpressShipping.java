package com.loja.domain.strategies.shipping;

import com.loja.domain.entities.Money;
import com.loja.domain.entities.Region;
import com.loja.domain.entities.ShippingMethod;
import com.loja.infrastructure.TransLogClient;

public class ExpressShipping implements ShippingStrategy {
    private final TransLogClient transLogClient;

    public ExpressShipping(TransLogClient transLogClient) {
        this.transLogClient = transLogClient;
    }

    @Override
    public Money calculateCost(String zipCode, double weightKg) throws ShippingUnavailableException {
        try {
            TransLogClient.TransLogQuote quote = transLogClient.getQuote(
                    zipCode, weightKg, TransLogClient.TransLogService.EXPRESS);
            return Money.of(quote.getPriceReais());
        } catch (TransLogClient.ZipNotServedException e) {
            throw new ShippingUnavailableException("ZIP not served", e);
        } catch (TransLogClient.TransLogException e) {
            throw new ShippingUnavailableException("Carrier unavailable", e);
        }
    }

    @Override
    public int calculateDeliveryDays(Region region) {
        try {
            TransLogClient.TransLogQuote quote = transLogClient.getQuote(
                    "00000000", 1.0, TransLogClient.TransLogService.EXPRESS);
            int baseDays = quote.getDeliveryDays();
            return baseDays + getRegionalDelay(region);
        } catch (Exception e) {
            return 2 + getRegionalDelay(region);
        }
    }

    private int getRegionalDelay(Region region) {
        return switch (region) {
            case NORTH -> 3;
            case NORTHEAST -> 2;
            default -> 0;
        };
    }

    @Override
    public boolean isAvailable(double weightKg, String zipCode) {
        try {
            calculateCost(zipCode, weightKg);
            return true;
        } catch (ShippingUnavailableException e) {
            return false;
        }
    }

    @Override
    public ShippingMethod getMethod() {
        return ShippingMethod.EXPRESS;
    }
}
