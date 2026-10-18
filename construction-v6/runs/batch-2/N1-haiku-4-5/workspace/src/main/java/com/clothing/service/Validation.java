package com.clothing.service;

import com.clothing.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class Validation {
    private static final Logger logger = LoggerFactory.getLogger(Validation.class);
    private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{8}$");

    public record ValidationError(String code, int httpStatus) {}

    public static ValidationError validateCheckout(CheckoutRequest req, ProductRepository products) {
        if (req.customer() == null || req.customer().cpf() == null || req.customer().cpf().isEmpty() ||
            req.customer().email() == null || req.customer().email().isEmpty()) {
            return new ValidationError("INVALID_ORDER", 422);
        }

        if (req.items() == null || req.items().isEmpty()) {
            return new ValidationError("INVALID_ORDER", 422);
        }

        Set<String> seenSkus = new HashSet<>();
        for (OrderItem item : req.items()) {
            if (item.sku() == null || item.sku().isEmpty() || item.quantity() <= 0) {
                return new ValidationError("INVALID_ORDER", 422);
            }
            if (!seenSkus.add(item.sku())) {
                return new ValidationError("INVALID_ORDER", 422);
            }
        }

        for (OrderItem item : req.items()) {
            Product product = products.findBySku(item.sku());
            if (product == null) {
                return new ValidationError("UNKNOWN_PRODUCT", 422);
            }
            if (product.discontinued()) {
                return new ValidationError("PRODUCT_DISCONTINUED", 422);
            }
            if (product.stock() < item.quantity()) {
                return new ValidationError("INSUFFICIENT_STOCK", 422);
            }
        }

        if (req.clubTier() == null) {
            return new ValidationError("INVALID_CLUB_TIER", 422);
        }

        if (req.region() == null) {
            return new ValidationError("INVALID_REGION", 422);
        }

        if (req.zipCode() == null || !ZIP_PATTERN.matcher(req.zipCode()).matches()) {
            return new ValidationError("INVALID_ZIP_CODE", 422);
        }

        if (req.shippingMethod() == null) {
            return new ValidationError("INVALID_SHIPPING_METHOD", 422);
        }

        double totalWeightKg = calculateTotalWeight(req.items(), products);
        if (req.shippingMethod() == ShippingMethod.COURIER && totalWeightKg > 5) {
            return new ValidationError("SHIPPING_METHOD_UNAVAILABLE", 422);
        }

        if (req.shippingMethod() == ShippingMethod.ECONOMY || req.shippingMethod() == ShippingMethod.EXPRESS) {
            try {
                String service = req.shippingMethod() == ShippingMethod.ECONOMY ? "STD" : "EXP";
                var quote = TransLogClient.quote(req.zipCode(), totalWeightKg, service);
                if (quote == null) {
                    return new ValidationError("SHIPPING_METHOD_UNAVAILABLE", 422);
                }
            } catch (TransLogClient.CarrierUnavailableException e) {
                return new ValidationError("CARRIER_UNAVAILABLE", 503);
            }
        }

        if (req.coupon() != null && !req.coupon().isEmpty()) {
            if (!isValidCoupon(req.coupon())) {
                return new ValidationError("INVALID_COUPON", 422);
            }
        }

        if (req.paymentMethod() == null) {
            return new ValidationError("INVALID_PAYMENT_METHOD", 422);
        }

        int installments = req.getInstallmentsOrDefault();
        if (req.paymentMethod() == PaymentMethod.PIX || req.paymentMethod() == PaymentMethod.BANK_SLIP) {
            if (installments != 1) {
                return new ValidationError("INVALID_INSTALLMENTS", 422);
            }
        } else if (req.paymentMethod() == PaymentMethod.CREDIT_CARD) {
            if (installments < 1 || installments > 12) {
                return new ValidationError("INVALID_INSTALLMENTS", 422);
            }
        }

        return null;
    }

    public static ValidationError validateCoupon(String coupon, CheckoutRequest req, ProductRepository products, CustomerRepository customers) {
        if (coupon == null || coupon.isEmpty()) {
            return null;
        }

        if (!isValidCoupon(coupon)) {
            return new ValidationError("INVALID_COUPON", 422);
        }

        switch (coupon) {
            case "WELCOME10":
                var existingOrders = customers.findCompletedOrders(req.customer().cpf());
                if (!existingOrders.isEmpty()) {
                    return new ValidationError("COUPON_NOT_APPLICABLE", 422);
                }
                break;

            case "MINUS50":
                var subtotal = calculateSubtotal(req.items(), products);
                if (subtotal.compareTo(new java.math.BigDecimal("300.00")) < 0) {
                    return new ValidationError("COUPON_NOT_APPLICABLE", 422);
                }
                break;

            case "FREESHIPPING":
                if (req.shippingMethod() == ShippingMethod.STORE_PICKUP) {
                    return new ValidationError("COUPON_NOT_APPLICABLE", 422);
                }
                break;

            case "BUY3PAY2":
                break;
        }

        return null;
    }

    public static ValidationError validatePaymentMethodForTotal(PaymentMethod method, java.math.BigDecimal total) {
        if (method == PaymentMethod.BANK_SLIP && total.compareTo(new java.math.BigDecimal("1000.00")) > 0) {
            return new ValidationError("PAYMENT_METHOD_UNAVAILABLE", 422);
        }
        return null;
    }

    private static boolean isValidCoupon(String code) {
        return code != null && (code.equals("WELCOME10") || code.equals("MINUS50") ||
               code.equals("FREESHIPPING") || code.equals("BUY3PAY2"));
    }

    private static double calculateTotalWeight(List<OrderItem> items, ProductRepository products) {
        double total = 0;
        for (OrderItem item : items) {
            Product product = products.findBySku(item.sku());
            if (product != null) {
                total += product.weightKg().doubleValue() * item.quantity();
            }
        }
        return total;
    }

    private static java.math.BigDecimal calculateSubtotal(List<OrderItem> items, ProductRepository products) {
        var subtotal = new java.math.BigDecimal("0");
        for (OrderItem item : items) {
            Product product = products.findBySku(item.sku());
            if (product != null) {
                subtotal = Money.add(subtotal, Money.multiply(product.price(), new java.math.BigDecimal(item.quantity())));
            }
        }
        return subtotal;
    }
}
