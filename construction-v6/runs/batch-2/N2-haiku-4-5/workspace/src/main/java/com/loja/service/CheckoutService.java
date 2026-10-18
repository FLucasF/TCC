package com.loja.service;

import com.loja.model.*;
import com.loja.repository.*;
import com.loja.strategy.*;
import com.loja.shipping.*;
import com.loja.util.Money;
import java.util.*;

public class CheckoutService {
    private final ProductRepository productRepo;
    private final OrderService orderService;

    public CheckoutService(ProductRepository productRepo, OrderService orderService) {
        this.productRepo = productRepo;
        this.orderService = orderService;
    }

    public OrderSummary calculateSummary(CheckoutRequest request) throws CheckoutException {
        validateRequest(request);

        Double productsSubtotal = calculateProductsSubtotal(request.items);

        ShippingInfo shippingInfo = calculateShipping(request, productsSubtotal);
        Double shippingCost = shippingInfo.cost;
        Integer deliveryDays = shippingInfo.days;

        ClubBenefitsStrategy clubBenefits = ClubFactory.create(request.clubTier);
        Double shippingCostForDiscount = shippingCost;
        if (clubBenefits.freeShipping(request.shippingMethod.name())) {
            shippingCostForDiscount = 0.0;
        }

        Double couponDiscount = calculateCouponDiscount(request, productsSubtotal, shippingCostForDiscount);
        couponDiscount = applyCouponCap(couponDiscount, productsSubtotal);

        Double insurance = calculateInsurance(productsSubtotal, request.region);

        Double adjustedShippingCost = shippingCost;
        if (clubBenefits.freeShipping(request.shippingMethod.name())) {
            adjustedShippingCost = 0.0;
        }

        Double subtotalBeforePayment = productsSubtotal - couponDiscount + adjustedShippingCost + insurance;
        Double paymentAdjustment = calculatePaymentAdjustment(request, subtotalBeforePayment);

        Double finalTotal = Money.round(subtotalBeforePayment + paymentAdjustment);

        Integer installments = request.installments != null ? request.installments : 1;
        Double installmentAmount = calculateInstallmentAmount(finalTotal, installments);

        Double nextPurchaseCredit = calculateCredit(productsSubtotal, clubBenefits);

        Boolean gift = clubBenefits.giftForLargeOrder(productsSubtotal);

        return new OrderSummary(
            productsSubtotal,
            couponDiscount,
            adjustedShippingCost,
            deliveryDays,
            insurance,
            paymentAdjustment,
            finalTotal,
            installments,
            installmentAmount,
            nextPurchaseCredit,
            gift
        );
    }

    private void validateRequest(CheckoutRequest request) throws CheckoutException {
        if (request.items == null || request.items.isEmpty()) {
            throw new CheckoutException("INVALID_ORDER");
        }

        Set<String> skus = new HashSet<>();
        for (OrderItem item : request.items) {
            if (item.sku == null || item.sku.isEmpty()) {
                throw new CheckoutException("INVALID_ORDER");
            }
            if (item.quantity == null || item.quantity <= 0) {
                throw new CheckoutException("INVALID_ORDER");
            }
            if (skus.contains(item.sku)) {
                throw new CheckoutException("INVALID_ORDER");
            }
            skus.add(item.sku);
        }

        if (request.customer == null || request.customer.cpf == null || request.customer.email == null) {
            throw new CheckoutException("INVALID_ORDER");
        }

        for (OrderItem item : request.items) {
            Product product = productRepo.findBySku(item.sku);
            if (product == null) {
                throw new CheckoutException("UNKNOWN_PRODUCT");
            }
            if (product.discontinued) {
                throw new CheckoutException("PRODUCT_DISCONTINUED");
            }
            if (product.stock < item.quantity) {
                throw new CheckoutException("INSUFFICIENT_STOCK");
            }
        }

        if (request.clubTier == null) {
            throw new CheckoutException("INVALID_CLUB_TIER");
        }

        if (request.region == null) {
            throw new CheckoutException("INVALID_REGION");
        }

        if (request.zipCode == null || request.zipCode.length() != 8) {
            throw new CheckoutException("INVALID_ZIP_CODE");
        }

        if (request.shippingMethod == null) {
            throw new CheckoutException("INVALID_SHIPPING_METHOD");
        }

        if (request.paymentMethod == null) {
            throw new CheckoutException("INVALID_PAYMENT_METHOD");
        }

        if (request.installments == null) {
            request.installments = 1;
        }

        PaymentStrategy paymentStrategy = PaymentFactory.create(request.paymentMethod);
        if (!paymentStrategy.isApplicable(0.0, request.installments)) {
            throw new CheckoutException("INVALID_INSTALLMENTS");
        }

        validateCoupon(request);
        validateShippingMethod(request);
    }

    private void validateCoupon(CheckoutRequest request) throws CheckoutException {
        if (request.coupon != null && !request.coupon.isEmpty()) {
            CouponFactory factory = new CouponFactory(orderService.getOrderRepo());
            if (!factory.isValidCoupon(request.coupon)) {
                throw new CheckoutException("INVALID_COUPON");
            }
        }
    }

    private void validateShippingMethod(CheckoutRequest request) throws CheckoutException {
        if (request.shippingMethod == ShippingMethod.COURIER) {
            Double totalWeight = calculateTotalWeight(request.items);
            if (totalWeight > 5.0) {
                throw new CheckoutException("SHIPPING_METHOD_UNAVAILABLE");
            }
        }

        if (request.shippingMethod == ShippingMethod.ECONOMY || request.shippingMethod == ShippingMethod.EXPRESS) {
            String translogUrl = System.getenv("TRANSLOG_URL");
            if (translogUrl == null || translogUrl.isEmpty()) {
                throw new CheckoutException("CARRIER_UNAVAILABLE");
            }

            try {
                ShippingCarrier carrier = new com.loja.shipping.TransLogAdapter(translogUrl);
                Double weightGrams = calculateTotalWeight(request.items) * 1000;
                ShippingQuote quote = carrier.getQuote(request.zipCode, weightGrams);

                if (quote.error != null) {
                    if ("ZIP_NOT_SERVED".equals(quote.error)) {
                        throw new CheckoutException("SHIPPING_METHOD_UNAVAILABLE");
                    }
                    throw new CheckoutException("CARRIER_UNAVAILABLE");
                }
            } catch (CheckoutException e) {
                throw e;
            } catch (Exception e) {
                throw new CheckoutException("CARRIER_UNAVAILABLE");
            }
        }
    }

    private Double calculateProductsSubtotal(List<OrderItem> items) {
        Double subtotal = 0.0;
        for (OrderItem item : items) {
            Product product = productRepo.findBySku(item.sku);
            if (product != null) {
                subtotal += product.price * item.quantity;
            }
        }
        return Money.round(subtotal);
    }

    private Double calculateCouponDiscount(CheckoutRequest request, Double productsSubtotal, Double shippingCost) throws CheckoutException {
        if (request.coupon == null || request.coupon.isEmpty()) {
            return 0.0;
        }

        CouponFactory factory = new CouponFactory(orderService.getOrderRepo());
        CouponStrategy strategy = factory.create(request.coupon);

        if (strategy == null) {
            throw new CheckoutException("INVALID_COUPON");
        }

        ProductProvider provider = sku -> productRepo.findBySku(sku);
        if (!strategy.isApplicable(request.customer.cpf, productsSubtotal, request.items, request.shippingMethod, provider)) {
            throw new CheckoutException("COUPON_NOT_APPLICABLE");
        }

        if ("FREESHIPPING".equals(request.coupon)) {
            return Money.round(shippingCost);
        }

        Double discount = strategy.calculateDiscount(productsSubtotal, request.items, provider);
        return discount != null ? discount : 0.0;
    }

    private Double applyCouponCap(Double discount, Double productsSubtotal) {
        Double maxDiscount = Money.round(productsSubtotal * 0.30);
        if (discount > maxDiscount) {
            return maxDiscount;
        }
        return discount;
    }

    private ShippingInfo calculateShipping(CheckoutRequest request, Double productsSubtotal) throws CheckoutException {
        switch (request.shippingMethod) {
            case STORE_PICKUP:
                return new ShippingInfo(0.0, 1);
            case COURIER:
                return new ShippingInfo(18.0, 0);
            case ECONOMY, EXPRESS:
                return getTranslogShipping(request);
        }
        throw new CheckoutException("INVALID_SHIPPING_METHOD");
    }

    private ShippingInfo getTranslogShipping(CheckoutRequest request) throws CheckoutException {
        String translogUrl = System.getenv("TRANSLOG_URL");
        com.loja.shipping.TransLogAdapter adapter = new com.loja.shipping.TransLogAdapter(translogUrl);

        Double weightGrams = calculateTotalWeight(request.items) * 1000;
        String service = request.shippingMethod == ShippingMethod.ECONOMY ? "STD" : "EXP";

        ShippingQuote quote = adapter.getQuoteWithService(request.zipCode, weightGrams, service);

        if (quote.error != null) {
            throw new CheckoutException("SHIPPING_METHOD_UNAVAILABLE");
        }

        Double shippingCost = Money.round(quote.priceCents / 100.0);
        Integer deliveryDays = (int) Math.ceil(quote.etaHours / 24.0);

        int extraDays = getExtraDaysForRegion(request.region);
        deliveryDays += extraDays;

        return new ShippingInfo(shippingCost, deliveryDays);
    }

    private int getExtraDaysForRegion(Region region) {
        return switch (region) {
            case NORTH -> 3;
            case NORTHEAST -> 2;
            default -> 0;
        };
    }

    private Double calculateInsurance(Double productsSubtotal, Region region) {
        Double percentage = getInsurancePercentage(region);
        Double insurance = Money.round(productsSubtotal * percentage);

        Double limit = getInsuranceLimit(region);
        if (limit != null && insurance > limit) {
            insurance = limit;
        }

        return insurance;
    }

    private Double getInsurancePercentage(Region region) {
        return switch (region) {
            case SOUTHEAST, SOUTH -> 0.01;
            case MIDWEST -> 0.015;
            case NORTH -> 0.025;
            case NORTHEAST -> 0.02;
        };
    }

    private Double getInsuranceLimit(Region region) {
        return switch (region) {
            case NORTH, NORTHEAST -> 30.0;
            default -> null;
        };
    }

    private Double calculatePaymentAdjustment(CheckoutRequest request, Double subtotal) throws CheckoutException {
        PaymentStrategy strategy = PaymentFactory.create(request.paymentMethod);

        Integer installments = request.installments != null ? request.installments : 1;

        if (!strategy.isApplicable(subtotal, installments)) {
            throw new CheckoutException("PAYMENT_METHOD_UNAVAILABLE");
        }

        if (request.paymentMethod == PaymentMethod.PIX && request.coupon != null) {
            return 0.0;
        }

        return strategy.calculateAdjustment(subtotal, installments);
    }

    private Double calculateInstallmentAmount(Double finalTotal, Integer installments) {
        return Money.round(finalTotal / installments);
    }

    private Double calculateCredit(Double productsSubtotal, ClubBenefitsStrategy benefits) {
        return Money.round(productsSubtotal * benefits.getCreditPercentage());
    }

    private Double calculateTotalWeight(List<OrderItem> items) {
        Double weight = 0.0;
        for (OrderItem item : items) {
            Product product = productRepo.findBySku(item.sku);
            if (product != null) {
                weight += product.weightKg * item.quantity;
            }
        }
        return weight;
    }

    private static class ShippingInfo {
        Double cost;
        Integer days;

        ShippingInfo(Double cost, Integer days) {
            this.cost = cost;
            this.days = days;
        }
    }
}
