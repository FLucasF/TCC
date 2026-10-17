package com.loja.orders.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

                                                       
public final class Checkout {

    private String cpf = "12345678909";
    private final Map<String, Integer> items = new LinkedHashMap<>();
    private String zipCode = "01310100";
    private String region = "SOUTHEAST";
    private String shippingMethod = "STORE_PICKUP";
    private String coupon;
    private String paymentMethod = "PIX";
    private Integer installments;
    private String clubTier = "BRONZE";

    public Checkout cpf(String cpf) {
        this.cpf = cpf;
        return this;
    }

    public Checkout item(String sku, int quantity) {
        items.put(sku, quantity);
        return this;
    }

    public Checkout region(String region) {
        this.region = region;
        return this;
    }

    public Checkout shipping(String shippingMethod) {
        this.shippingMethod = shippingMethod;
        return this;
    }

    public Checkout coupon(String coupon) {
        this.coupon = coupon;
        return this;
    }

    public Checkout payment(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public Checkout payment(String paymentMethod, int installments) {
        this.paymentMethod = paymentMethod;
        this.installments = installments;
        return this;
    }

    public Checkout tier(String clubTier) {
        this.clubTier = clubTier;
        return this;
    }

    public Checkout zipCode(String zipCode) {
        this.zipCode = zipCode;
        return this;
    }

    public String json() {
        String itemsJson = items.entrySet().stream()
                .map(e -> "{ \"sku\": \"" + e.getKey() + "\", \"quantity\": " + e.getValue() + " }")
                .collect(Collectors.joining(", ", "[", "]"));
        StringBuilder json = new StringBuilder("{")
                .append("\"customer\": { \"cpf\": \"").append(cpf).append("\", \"email\": \"ana@exemplo.com\" }, ")
                .append("\"items\": ").append(itemsJson);
        field(json, "zipCode", zipCode);
        field(json, "region", region);
        field(json, "shippingMethod", shippingMethod);
        field(json, "coupon", coupon);
        field(json, "paymentMethod", paymentMethod);
        if (installments != null) {
            json.append(", \"installments\": ").append(installments);
        }
        field(json, "clubTier", clubTier);
        return json.append("}").toString();
    }

    private static void field(StringBuilder json, String name, String value) {
        if (value != null) {
            json.append(", \"").append(name).append("\": \"").append(value).append("\"");
        }
    }

    @Override
    public String toString() {
        return json();
    }
}
