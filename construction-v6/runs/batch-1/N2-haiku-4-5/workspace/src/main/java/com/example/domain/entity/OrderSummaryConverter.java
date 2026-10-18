package com.example.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Converter(autoApply = true)
public class OrderSummaryConverter implements AttributeConverter<OrderSummary, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(OrderSummary attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("productsSubtotal", attribute.getProductsSubtotal());
            map.put("couponDiscount", attribute.getCouponDiscount());
            map.put("shippingCost", attribute.getShippingCost());
            map.put("deliveryDays", attribute.getDeliveryDays());
            map.put("insurance", attribute.getInsurance());
            map.put("paymentAdjustment", attribute.getPaymentAdjustment());
            map.put("finalTotal", attribute.getFinalTotal());
            map.put("installments", attribute.getInstallments());
            map.put("installmentAmount", attribute.getInstallmentAmount());
            map.put("nextPurchaseCredit", attribute.getNextPurchaseCredit());
            map.put("gift", attribute.getGift());
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OrderSummary convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(dbData, Map.class);
            return new OrderSummary(
                    new BigDecimal(map.get("productsSubtotal").toString()),
                    new BigDecimal(map.get("couponDiscount").toString()),
                    new BigDecimal(map.get("shippingCost").toString()),
                    ((Number) map.get("deliveryDays")).intValue(),
                    new BigDecimal(map.get("insurance").toString()),
                    new BigDecimal(map.get("paymentAdjustment").toString()),
                    new BigDecimal(map.get("finalTotal").toString()),
                    ((Number) map.get("installments")).intValue(),
                    new BigDecimal(map.get("installmentAmount").toString()),
                    new BigDecimal(map.get("nextPurchaseCredit").toString()),
                    (Boolean) map.get("gift")
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
