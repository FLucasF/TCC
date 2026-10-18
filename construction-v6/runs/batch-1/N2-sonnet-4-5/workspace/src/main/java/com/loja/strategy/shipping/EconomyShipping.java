package com.loja.strategy.shipping;

import com.loja.dto.CheckoutRequest;
import com.loja.external.TransLogClient;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class EconomyShipping implements ShippingStrategy {
    private final TransLogClient transLogClient;

    public EconomyShipping(TransLogClient transLogClient) {
        this.transLogClient = transLogClient;
    }

    @Override
    public String getCode() {
        return "ECONOMY";
    }

    @Override
    public ShippingResult calculate(CheckoutRequest request, BigDecimal totalWeightKg) {
        TransLogClient.TransLogQuote quote = transLogClient.getQuote(request.getZipCode(), totalWeightKg, "STD");

        if (!quote.isAvailable()) {
            throw new RuntimeException("TransLog unavailable or ZIP not served");
        }

        int extraDays = switch (request.getRegion()) {
            case "NORTH" -> 3;
            case "NORTHEAST" -> 2;
            default -> 0;
        };

        return new ShippingResult(quote.getPrice(), quote.getDeliveryDays() + extraDays);
    }

    @Override
    public boolean isAvailable(CheckoutRequest request, BigDecimal totalWeightKg) {
        TransLogClient.TransLogQuote quote = transLogClient.getQuote(request.getZipCode(), totalWeightKg, "STD");
        return quote.isAvailable();
    }
}
