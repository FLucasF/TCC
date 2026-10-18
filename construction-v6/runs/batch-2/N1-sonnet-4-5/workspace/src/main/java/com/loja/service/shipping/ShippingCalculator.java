package com.loja.service.shipping;

import com.loja.domain.*;
import com.loja.util.ApiException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ShippingCalculator {
    private final TransLogClient transLogClient;

    public ShippingCalculator(TransLogClient transLogClient) {
        this.transLogClient = transLogClient;
    }

    public ShippingResult calculate(CheckoutRequest request, Map<String, Product> productMap) {
        BigDecimal totalWeight = calculateTotalWeight(request.getItems(), productMap);

        return switch (request.getShippingMethod()) {
            case STORE_PICKUP -> new ShippingResult(BigDecimal.ZERO, 1);
            case COURIER -> {
                if (totalWeight.compareTo(new BigDecimal("5.0")) > 0) {
                    throw new ApiException("SHIPPING_METHOD_UNAVAILABLE", 422);
                }
                yield new ShippingResult(new BigDecimal("18.00"), 0);
            }
            case ECONOMY, EXPRESS -> {
                try {
                    ShippingResult result = transLogClient.quote(
                        request.getZipCode(),
                        totalWeight,
                        request.getShippingMethod(),
                        request.getRegion()
                    );
                    if (result == null) {
                        throw new ApiException("SHIPPING_METHOD_UNAVAILABLE", 422);
                    }
                    yield result;
                } catch (RuntimeException e) {
                    if (e.getMessage().equals("CARRIER_UNAVAILABLE")) {
                        throw new ApiException("CARRIER_UNAVAILABLE", 503);
                    }
                    throw e;
                }
            }
        };
    }

    private BigDecimal calculateTotalWeight(List<OrderItem> items, Map<String, Product> productMap) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Product product = productMap.get(item.getSku());
            total = total.add(product.getWeightKg().multiply(new BigDecimal(item.getQuantity())));
        }
        return total;
    }
}
