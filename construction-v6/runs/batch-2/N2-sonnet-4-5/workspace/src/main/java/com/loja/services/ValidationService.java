package com.loja.services;

import com.loja.domain.entities.*;
import com.loja.domain.strategies.coupon.CouponStrategy;
import com.loja.domain.strategies.payment.PaymentMethodStrategy;
import com.loja.domain.strategies.shipping.ShippingStrategy;
import com.loja.repositories.OrderRepository;
import com.loja.repositories.ProductRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValidationService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final Map<ShippingMethod, ShippingStrategy> shippingStrategies;
    private final Map<CouponCode, CouponStrategy> couponStrategies;
    private final Map<PaymentMethod, PaymentMethodStrategy> paymentMethodStrategies;

    public ValidationService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            Map<ShippingMethod, ShippingStrategy> shippingStrategies,
            Map<CouponCode, CouponStrategy> couponStrategies,
            Map<PaymentMethod, PaymentMethodStrategy> paymentMethodStrategies) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.shippingStrategies = shippingStrategies;
        this.couponStrategies = couponStrategies;
        this.paymentMethodStrategies = paymentMethodStrategies;
    }

    public void validateCheckoutRequest(CheckoutService.CheckoutRequest request) throws ValidationException {
        validateOrderStructure(request);
        validateProducts(request.items());
        validateClubTier(request.clubTier());
        validateRegion(request.region());
        validateZipCode(request.zipCode());
        validateShippingMethod(request.shippingMethod(), request.items(), request.zipCode());
        validateCoupon(request);
        validatePaymentMethod(request);
    }

    private void validateOrderStructure(CheckoutService.CheckoutRequest request) throws ValidationException {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ValidationException("INVALID_ORDER", "Cart is empty");
        }

        if (request.customerCpf() == null || request.customerCpf().isBlank()) {
            throw new ValidationException("INVALID_ORDER", "Customer CPF is required");
        }

        Set<String> skus = new HashSet<>();
        for (OrderItem item : request.items()) {
            if (item.getSku() == null || item.getSku().isBlank()) {
                throw new ValidationException("INVALID_ORDER", "Item SKU is required");
            }

            if (item.getQuantity() <= 0) {
                throw new ValidationException("INVALID_ORDER", "Item quantity must be positive");
            }

            if (skus.contains(item.getSku())) {
                throw new ValidationException("INVALID_ORDER", "Duplicate SKU in cart: " + item.getSku());
            }
            skus.add(item.getSku());
        }
    }

    private void validateProducts(List<OrderItem> items) throws ValidationException {
        for (OrderItem item : items) {
            Product product = productRepository.findBySku(item.getSku());

            if (product == null) {
                throw new ValidationException("UNKNOWN_PRODUCT", "Product not found: " + item.getSku());
            }

            if (product.isDiscontinued()) {
                throw new ValidationException("PRODUCT_DISCONTINUED", "Product discontinued: " + item.getSku());
            }

            if (!product.hasStock(item.getQuantity())) {
                throw new ValidationException("INSUFFICIENT_STOCK", "Insufficient stock for product: " + item.getSku());
            }
        }
    }

    private void validateClubTier(ClubTier clubTier) throws ValidationException {
        if (clubTier == null) {
            throw new ValidationException("INVALID_CLUB_TIER", "Club tier is required");
        }
    }

    private void validateRegion(Region region) throws ValidationException {
        if (region == null) {
            throw new ValidationException("INVALID_REGION", "Region is required");
        }
    }

    private void validateZipCode(String zipCode) throws ValidationException {
        if (zipCode == null || !zipCode.matches("\\d{8}")) {
            throw new ValidationException("INVALID_ZIP_CODE", "Invalid ZIP code format");
        }
    }

    private void validateShippingMethod(ShippingMethod shippingMethod, List<OrderItem> items, String zipCode) throws ValidationException {
        if (shippingMethod == null) {
            throw new ValidationException("INVALID_SHIPPING_METHOD", "Shipping method is required");
        }

        ShippingStrategy strategy = shippingStrategies.get(shippingMethod);
        if (strategy == null) {
            throw new ValidationException("INVALID_SHIPPING_METHOD", "Invalid shipping method");
        }

        double totalWeight = calculateTotalWeight(items);

        try {
            Money cost = strategy.calculateCost(zipCode, totalWeight);
            if (!strategy.isAvailable(totalWeight, zipCode)) {
                throw new ValidationException("SHIPPING_METHOD_UNAVAILABLE", "Shipping method not available");
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("unavailable")) {
                throw new ValidationException("CARRIER_UNAVAILABLE", "Carrier unavailable");
            }
            if (e.getMessage() != null && e.getMessage().contains("not served")) {
                throw new ValidationException("SHIPPING_METHOD_UNAVAILABLE", "Shipping method not available for this location");
            }
            throw new ValidationException("CARRIER_UNAVAILABLE", "Carrier unavailable");
        }
    }

    private void validateCoupon(CheckoutService.CheckoutRequest request) throws ValidationException {
        if (request.coupon() == null) {
            return;
        }

        CouponStrategy strategy = couponStrategies.get(request.coupon());
        if (strategy == null) {
            throw new ValidationException("INVALID_COUPON", "Invalid coupon code");
        }

        Money productsSubtotal = calculateProductsSubtotal(request.items());
        Money shippingCost = Money.zero();
        try {
            shippingCost = shippingStrategies.get(request.shippingMethod())
                    .calculateCost(request.zipCode(), calculateTotalWeight(request.items()));
        } catch (Exception e) {
        }

        if (!strategy.isApplicable(
                productsSubtotal,
                shippingCost,
                request.items(),
                request.customerCpf(),
                request.shippingMethod())) {
            throw new ValidationException("COUPON_NOT_APPLICABLE", "Coupon not applicable to this order");
        }
    }

    private void validatePaymentMethod(CheckoutService.CheckoutRequest request) throws ValidationException {
        if (request.paymentMethod() == null) {
            throw new ValidationException("INVALID_PAYMENT_METHOD", "Payment method is required");
        }

        PaymentMethodStrategy strategy = paymentMethodStrategies.get(request.paymentMethod());
        if (strategy == null) {
            throw new ValidationException("INVALID_PAYMENT_METHOD", "Invalid payment method");
        }

        int installments = request.installments();
        if (request.paymentMethod() == PaymentMethod.PIX || request.paymentMethod() == PaymentMethod.BANK_SLIP) {
            if (installments != 1 && installments != 0) {
                throw new ValidationException("INVALID_INSTALLMENTS", "PIX and bank slip must be paid in full");
            }
        } else if (request.paymentMethod() == PaymentMethod.CREDIT_CARD) {
            if (installments < 1 || installments > 12) {
                throw new ValidationException("INVALID_INSTALLMENTS", "Credit card installments must be between 1 and 12");
            }
        }

        Money productsSubtotal = calculateProductsSubtotal(request.items());
        if (!strategy.isAvailable(productsSubtotal)) {
            throw new ValidationException("PAYMENT_METHOD_UNAVAILABLE", "Payment method not available for this order total");
        }
    }

    private Money calculateProductsSubtotal(List<OrderItem> items) {
        Money subtotal = Money.zero();
        for (OrderItem item : items) {
            Product product = productRepository.findBySku(item.getSku());
            subtotal = subtotal.add(product.getPrice().multiply(item.getQuantity()));
        }
        return subtotal;
    }

    private double calculateTotalWeight(List<OrderItem> items) {
        double totalWeight = 0;
        for (OrderItem item : items) {
            Product product = productRepository.findBySku(item.getSku());
            totalWeight += product.getWeightKg() * item.getQuantity();
        }
        return totalWeight;
    }

    public static class ValidationException extends Exception {
        private final String errorCode;

        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
