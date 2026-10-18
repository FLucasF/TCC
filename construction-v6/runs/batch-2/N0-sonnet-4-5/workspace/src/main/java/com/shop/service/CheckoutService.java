package com.shop.service;

import com.shop.dto.CheckoutRequest;
import com.shop.dto.CheckoutSummary;
import com.shop.exception.BusinessException;
import com.shop.model.OrderItem;
import com.shop.model.Product;
import com.shop.model.enums.CouponCode;
import com.shop.model.enums.PaymentMethod;
import com.shop.model.enums.Region;
import com.shop.model.enums.ShippingMethod;
import com.shop.repository.OrderRepository;
import com.shop.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class CheckoutService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShippingService shippingService;
    private final CouponService couponService;
    private final ClubService clubService;
    private final PaymentService paymentService;

    public CheckoutService(ProductRepository productRepository, OrderRepository orderRepository,
                          ShippingService shippingService, CouponService couponService,
                          ClubService clubService, PaymentService paymentService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.shippingService = shippingService;
        this.couponService = couponService;
        this.clubService = clubService;
        this.paymentService = paymentService;
    }

    public CheckoutSummary calculateSummary(CheckoutRequest request, Connection conn) throws SQLException {
        validateRequest(request, conn);

        Set<String> skus = new HashSet<>();
        for (OrderItem item : request.getItems()) {
            skus.add(item.getSku());
        }
        Map<String, Product> products = productRepository.findBySkus(skus);

        BigDecimal productsSubtotal = calculateProductsSubtotal(request.getItems(), products);

        BigDecimal totalWeight = calculateTotalWeight(request.getItems(), products);

        ShippingService.ShippingQuote shipping = shippingService.calculateShipping(
                request.getShippingMethod(), request.getZipCode(), totalWeight, request.getRegion());

        BigDecimal shippingCost = shipping.cost;
        int deliveryDays = shipping.deliveryDays;

        if (clubService.hasFreeShipping(request.getClubTier())
                && request.getShippingMethod() != ShippingMethod.COURIER) {
            shippingCost = BigDecimal.ZERO;
        }

        boolean isFirstPurchase = false;
        if (conn != null) {
            long paidOrders = orderRepository.countPaidOrdersByCpf(conn, request.getCustomer().getCpf());
            isFirstPurchase = paidOrders == 0;
        }

        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request.getCoupon() != null) {
            BigDecimal discount = couponService.calculateDiscount(
                    request.getCoupon(), productsSubtotal, shippingCost, request.getItems(),
                    products, request.getShippingMethod(), isFirstPurchase);

            if (discount == null) {
                throw new BusinessException("COUPON_NOT_APPLICABLE", 422);
            }

            couponDiscount = discount;
        }

        BigDecimal insurance = calculateInsurance(productsSubtotal, request.getRegion());

        BigDecimal orderTotal = productsSubtotal.subtract(couponDiscount).add(shippingCost).add(insurance);

        int installments = request.getInstallments() != null ? request.getInstallments() : 1;

        PaymentService.PaymentCalculation payment = paymentService.calculatePayment(
                request.getPaymentMethod(), orderTotal, installments,
                request.getCoupon(), couponDiscount);

        BigDecimal nextPurchaseCredit = clubService.calculateCredit(request.getClubTier(), productsSubtotal);
        boolean gift = clubService.hasGift(request.getClubTier(), productsSubtotal);

        CheckoutSummary summary = new CheckoutSummary();
        summary.setProductsSubtotal(productsSubtotal);
        summary.setCouponDiscount(couponDiscount);
        summary.setShippingCost(shippingCost);
        summary.setDeliveryDays(deliveryDays);
        summary.setInsurance(insurance);
        summary.setPaymentAdjustment(payment.adjustment);
        summary.setFinalTotal(payment.finalTotal);
        summary.setInstallments(payment.installments);
        summary.setInstallmentAmount(payment.installmentAmount);
        summary.setNextPurchaseCredit(nextPurchaseCredit);
        summary.setGift(gift);

        return summary;
    }

    private void validateRequest(CheckoutRequest request, Connection conn) throws SQLException {
        if (request.getCustomer() == null || request.getCustomer().getCpf() == null
                || request.getCustomer().getEmail() == null) {
            throw new BusinessException("INVALID_ORDER", 422);
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("INVALID_ORDER", 422);
        }

        Set<String> seenSkus = new HashSet<>();
        for (OrderItem item : request.getItems()) {
            if (item.getSku() == null || item.getQuantity() <= 0) {
                throw new BusinessException("INVALID_ORDER", 422);
            }
            if (seenSkus.contains(item.getSku())) {
                throw new BusinessException("INVALID_ORDER", 422);
            }
            seenSkus.add(item.getSku());
        }

        Map<String, Product> products = productRepository.findBySkus(seenSkus);
        for (OrderItem item : request.getItems()) {
            Product product = products.get(item.getSku());
            if (product == null) {
                throw new BusinessException("UNKNOWN_PRODUCT", 422);
            }
            if (product.isDiscontinued()) {
                throw new BusinessException("PRODUCT_DISCONTINUED", 422);
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK", 422);
            }
        }

        if (request.getClubTier() == null) {
            throw new BusinessException("INVALID_CLUB_TIER", 422);
        }

        if (request.getRegion() == null) {
            throw new BusinessException("INVALID_REGION", 422);
        }

        if (request.getZipCode() == null || !request.getZipCode().matches("\\d{8}")) {
            throw new BusinessException("INVALID_ZIP_CODE", 422);
        }

        if (request.getShippingMethod() == null) {
            throw new BusinessException("INVALID_SHIPPING_METHOD", 422);
        }

        BigDecimal totalWeight = calculateTotalWeight(request.getItems(), products);
        if (request.getShippingMethod() == ShippingMethod.COURIER
                && totalWeight.compareTo(new BigDecimal("5.0")) > 0) {
            throw new BusinessException("SHIPPING_METHOD_UNAVAILABLE", 422);
        }

        if (request.getCoupon() != null) {
        }

        if (request.getPaymentMethod() == null) {
            throw new BusinessException("INVALID_PAYMENT_METHOD", 422);
        }

        int installments = request.getInstallments() != null ? request.getInstallments() : 1;
        switch (request.getPaymentMethod()) {
            case PIX, BANK_SLIP -> {
                if (installments != 1) {
                    throw new BusinessException("INVALID_INSTALLMENTS", 422);
                }
            }
            case CREDIT_CARD -> {
                if (installments < 1 || installments > 12) {
                    throw new BusinessException("INVALID_INSTALLMENTS", 422);
                }
            }
        }

        if (request.getPaymentMethod() == PaymentMethod.BANK_SLIP) {
            BigDecimal productsSubtotal = calculateProductsSubtotal(request.getItems(), products);
            ShippingService.ShippingQuote shipping = shippingService.calculateShipping(
                    request.getShippingMethod(), request.getZipCode(), totalWeight, request.getRegion());
            BigDecimal shippingCost = shipping.cost;
            if (clubService.hasFreeShipping(request.getClubTier())
                    && request.getShippingMethod() != ShippingMethod.COURIER) {
                shippingCost = BigDecimal.ZERO;
            }

            boolean isFirstPurchase = false;
            if (conn != null) {
                long paidOrders = orderRepository.countPaidOrdersByCpf(conn, request.getCustomer().getCpf());
                isFirstPurchase = paidOrders == 0;
            }

            BigDecimal couponDiscount = BigDecimal.ZERO;
            if (request.getCoupon() != null) {
                BigDecimal discount = couponService.calculateDiscount(
                        request.getCoupon(), productsSubtotal, shippingCost, request.getItems(),
                        products, request.getShippingMethod(), isFirstPurchase);

                if (discount != null) {
                    couponDiscount = discount;
                }
            }

            BigDecimal insurance = calculateInsurance(productsSubtotal, request.getRegion());
            BigDecimal orderTotal = productsSubtotal.subtract(couponDiscount).add(shippingCost).add(insurance);

            if (orderTotal.compareTo(new BigDecimal("1000.00")) > 0) {
                throw new BusinessException("PAYMENT_METHOD_UNAVAILABLE", 422);
            }
        }
    }

    private BigDecimal calculateProductsSubtotal(List<OrderItem> items, Map<String, Product> products) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Product product = products.get(item.getSku());
            BigDecimal itemTotal = product.getPrice()
                    .multiply(new BigDecimal(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_EVEN);
            subtotal = subtotal.add(itemTotal);
        }
        return subtotal;
    }

    private BigDecimal calculateTotalWeight(List<OrderItem> items, Map<String, Product> products) {
        BigDecimal weight = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Product product = products.get(item.getSku());
            BigDecimal itemWeight = product.getWeightKg()
                    .multiply(new BigDecimal(item.getQuantity()));
            weight = weight.add(itemWeight);
        }
        return weight;
    }

    private BigDecimal calculateInsurance(BigDecimal productsSubtotal, Region region) {
        BigDecimal percentage = switch (region) {
            case SOUTHEAST, SOUTH -> new BigDecimal("0.01");
            case MIDWEST -> new BigDecimal("0.015");
            case NORTH -> new BigDecimal("0.025");
            case NORTHEAST -> new BigDecimal("0.02");
        };

        BigDecimal insurance = productsSubtotal.multiply(percentage)
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal limit = switch (region) {
            case NORTH, NORTHEAST -> new BigDecimal("30.00");
            case SOUTHEAST, SOUTH, MIDWEST -> null;
        };

        if (limit != null && insurance.compareTo(limit) > 0) {
            return limit;
        }

        return insurance;
    }
}
