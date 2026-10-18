package com.loja.services;

import com.loja.domain.entities.*;
import com.loja.domain.strategies.club.ClubTierStrategy;
import com.loja.domain.strategies.coupon.CouponStrategy;
import com.loja.domain.strategies.payment.PaymentMethodStrategy;
import com.loja.domain.strategies.shipping.ShippingStrategy;
import com.loja.domain.strategies.shipping.ShippingUnavailableException;
import com.loja.repositories.ProductRepository;

import java.util.List;
import java.util.Map;

public class CheckoutService {
    private static final double MAX_COUPON_DISCOUNT_PERCENT = 0.30;

    private final ProductRepository productRepository;
    private final Map<ShippingMethod, ShippingStrategy> shippingStrategies;
    private final Map<CouponCode, CouponStrategy> couponStrategies;
    private final Map<ClubTier, ClubTierStrategy> clubTierStrategies;
    private final Map<PaymentMethod, PaymentMethodStrategy> paymentMethodStrategies;

    public CheckoutService(
            ProductRepository productRepository,
            Map<ShippingMethod, ShippingStrategy> shippingStrategies,
            Map<CouponCode, CouponStrategy> couponStrategies,
            Map<ClubTier, ClubTierStrategy> clubTierStrategies,
            Map<PaymentMethod, PaymentMethodStrategy> paymentMethodStrategies) {
        this.productRepository = productRepository;
        this.shippingStrategies = shippingStrategies;
        this.couponStrategies = couponStrategies;
        this.clubTierStrategies = clubTierStrategies;
        this.paymentMethodStrategies = paymentMethodStrategies;
    }

    public OrderSummary calculateSummary(CheckoutRequest request) throws ShippingUnavailableException {
        Money productsSubtotal = calculateProductsSubtotal(request.items());

        ShippingStrategy shippingStrategy = shippingStrategies.get(request.shippingMethod());
        Money rawShippingCost = shippingStrategy.calculateCost(request.zipCode(), calculateTotalWeight(request.items()));
        int baseDeliveryDays = shippingStrategy.calculateDeliveryDays(request.region());

        CouponStrategy couponStrategy = request.coupon() != null ? couponStrategies.get(request.coupon()) : null;
        Money couponDiscount = Money.zero();
        if (couponStrategy != null) {
            couponDiscount = couponStrategy.calculateDiscount(
                    productsSubtotal,
                    rawShippingCost,
                    request.items(),
                    request.customerCpf()
            );
            Money maxDiscount = productsSubtotal.multiply(MAX_COUPON_DISCOUNT_PERCENT);
            if (couponDiscount.isGreaterThan(maxDiscount)) {
                couponDiscount = maxDiscount;
            }
        }

        Money insurance = calculateInsurance(productsSubtotal, request.region());

        ClubTierStrategy clubStrategy = clubTierStrategies.get(request.clubTier());
        Money adjustedShippingCost = clubStrategy.adjustShippingCost(rawShippingCost, request.shippingMethod());

        Money orderTotal = productsSubtotal
                .subtract(couponDiscount)
                .add(adjustedShippingCost)
                .add(insurance);

        PaymentMethodStrategy paymentStrategy = paymentMethodStrategies.get(request.paymentMethod());
        int installments = paymentStrategy.calculateInstallments(request.installments());
        Money paymentAdjustment = paymentStrategy.calculateAdjustment(
                orderTotal,
                installments,
                request.coupon() != null
        );

        Money finalTotal = orderTotal.add(paymentAdjustment);
        Money installmentAmount = paymentStrategy.calculateInstallmentAmount(finalTotal, installments);

        Money nextPurchaseCredit = clubStrategy.calculateCredit(productsSubtotal);
        boolean gift = clubStrategy.hasGift(productsSubtotal);

        return new OrderSummary(
                productsSubtotal,
                couponDiscount,
                adjustedShippingCost,
                baseDeliveryDays,
                insurance,
                paymentAdjustment,
                finalTotal,
                installments,
                installmentAmount,
                nextPurchaseCredit,
                gift
        );
    }

    private Money calculateProductsSubtotal(List<OrderItem> items) {
        Money subtotal = Money.zero();
        for (OrderItem item : items) {
            Product product = productRepository.findBySku(item.getSku());
            Money itemTotal = product.getPrice().multiply(item.getQuantity());
            subtotal = subtotal.add(itemTotal);
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

    private Money calculateInsurance(Money productsSubtotal, Region region) {
        double percentage = switch (region) {
            case SOUTHEAST, SOUTH -> 0.01;
            case MIDWEST -> 0.015;
            case NORTHEAST -> 0.02;
            case NORTH -> 0.025;
        };

        Money insurance = productsSubtotal.multiply(percentage);

        Money limit = switch (region) {
            case NORTH, NORTHEAST -> Money.of(30.00);
            case SOUTHEAST, SOUTH, MIDWEST -> null;
        };

        if (limit != null && insurance.isGreaterThan(limit)) {
            return limit;
        }

        return insurance;
    }

    public record CheckoutRequest(
            String customerCpf,
            List<OrderItem> items,
            String zipCode,
            Region region,
            ShippingMethod shippingMethod,
            CouponCode coupon,
            PaymentMethod paymentMethod,
            int installments,
            ClubTier clubTier
    ) {
    }
}
