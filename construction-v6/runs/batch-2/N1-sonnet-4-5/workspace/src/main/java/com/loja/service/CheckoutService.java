package com.loja.service;

import com.loja.domain.*;
import com.loja.repository.OrderRepository;
import com.loja.repository.ProductRepository;
import com.loja.service.club.ClubBenefitsCalculator;
import com.loja.service.coupon.CouponService;
import com.loja.service.coupon.CouponStrategy;
import com.loja.service.insurance.InsuranceCalculator;
import com.loja.service.payment.PaymentAdjustmentCalculator;
import com.loja.service.shipping.ShippingCalculator;
import com.loja.service.shipping.ShippingResult;
import com.loja.util.ApiException;
import com.loja.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class CheckoutService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CouponService couponService;
    private final ShippingCalculator shippingCalculator;
    private final InsuranceCalculator insuranceCalculator;
    private final PaymentAdjustmentCalculator paymentAdjustmentCalculator;
    private final ClubBenefitsCalculator clubBenefitsCalculator;

    public CheckoutService(ProductRepository productRepository,
                          OrderRepository orderRepository,
                          CouponService couponService,
                          ShippingCalculator shippingCalculator,
                          InsuranceCalculator insuranceCalculator,
                          PaymentAdjustmentCalculator paymentAdjustmentCalculator,
                          ClubBenefitsCalculator clubBenefitsCalculator) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.couponService = couponService;
        this.shippingCalculator = shippingCalculator;
        this.insuranceCalculator = insuranceCalculator;
        this.paymentAdjustmentCalculator = paymentAdjustmentCalculator;
        this.clubBenefitsCalculator = clubBenefitsCalculator;
    }

    public CheckoutSummary calculateSummary(CheckoutRequest request) throws SQLException {
        return calculateSummary(request, null);
    }

    public CheckoutSummary calculateSummary(CheckoutRequest request, Connection conn) throws SQLException {
        validateRequest(request, conn);

        Set<String> skus = new HashSet<>();
        for (OrderItem item : request.getItems()) {
            skus.add(item.getSku());
        }
        Map<String, Product> productMap = productRepository.findBySkus(skus);

        BigDecimal productsSubtotal = calculateProductsSubtotal(request.getItems(), productMap);
        productsSubtotal = MoneyUtils.roundMoney(productsSubtotal);

        boolean isFirstPurchase = conn == null || !orderRepository.hasCompletedOrders(request.getCustomer().getCpf(), conn);

        BigDecimal couponDiscount = BigDecimal.ZERO;
        boolean hasCoupon = request.getCoupon() != null && !request.getCoupon().isEmpty();
        if (hasCoupon) {
            CouponStrategy coupon = couponService.getCoupon(request.getCoupon());
            if (!coupon.isApplicable(request, productMap, isFirstPurchase)) {
                throw new ApiException("COUPON_NOT_APPLICABLE", 422);
            }

            ShippingResult shippingForCoupon = shippingCalculator.calculate(request, productMap);
            BigDecimal preliminaryShipping = clubBenefitsCalculator.hasFreeShipping(request.getClubTier(), request.getShippingMethod())
                ? BigDecimal.ZERO
                : shippingForCoupon.getCost();

            couponDiscount = coupon.calculateDiscount(productsSubtotal, preliminaryShipping, request.getItems(), productMap);
            couponDiscount = couponService.applyCap(couponDiscount, productsSubtotal);
        }

        ShippingResult shippingResult = shippingCalculator.calculate(request, productMap);
        BigDecimal shippingCost = clubBenefitsCalculator.hasFreeShipping(request.getClubTier(), request.getShippingMethod())
            ? BigDecimal.ZERO
            : shippingResult.getCost();
        int deliveryDays = shippingResult.getDeliveryDays();

        BigDecimal insurance = insuranceCalculator.calculate(productsSubtotal, request.getRegion());

        BigDecimal orderTotal = productsSubtotal.subtract(couponDiscount).add(shippingCost).add(insurance);

        int installments = request.getInstallments() != null ? request.getInstallments() : 1;
        BigDecimal paymentAdjustment = paymentAdjustmentCalculator.calculate(orderTotal, request.getPaymentMethod(), installments, hasCoupon);

        BigDecimal finalTotal = orderTotal.add(paymentAdjustment);
        BigDecimal installmentAmount = paymentAdjustmentCalculator.calculateInstallmentAmount(finalTotal, installments);

        BigDecimal nextPurchaseCredit = clubBenefitsCalculator.calculateCredit(productsSubtotal, request.getClubTier());
        boolean gift = clubBenefitsCalculator.hasGift(request.getClubTier(), productsSubtotal);

        CheckoutSummary summary = new CheckoutSummary();
        summary.setProductsSubtotal(productsSubtotal);
        summary.setCouponDiscount(couponDiscount);
        summary.setShippingCost(shippingCost);
        summary.setDeliveryDays(deliveryDays);
        summary.setInsurance(insurance);
        summary.setPaymentAdjustment(paymentAdjustment);
        summary.setFinalTotal(finalTotal);
        summary.setInstallments(installments);
        summary.setInstallmentAmount(installmentAmount);
        summary.setNextPurchaseCredit(nextPurchaseCredit);
        summary.setGift(gift);

        return summary;
    }

    private void validateRequest(CheckoutRequest request, Connection conn) throws SQLException {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException("INVALID_ORDER", 422);
        }

        if (request.getCustomer() == null || request.getCustomer().getCpf() == null || request.getCustomer().getEmail() == null) {
            throw new ApiException("INVALID_ORDER", 422);
        }

        Set<String> seenSkus = new HashSet<>();
        for (OrderItem item : request.getItems()) {
            if (item.getSku() == null || item.getSku().isEmpty()) {
                throw new ApiException("INVALID_ORDER", 422);
            }
            if (item.getQuantity() <= 0) {
                throw new ApiException("INVALID_ORDER", 422);
            }
            if (!seenSkus.add(item.getSku())) {
                throw new ApiException("INVALID_ORDER", 422);
            }
        }

        Set<String> skus = new HashSet<>();
        for (OrderItem item : request.getItems()) {
            skus.add(item.getSku());
        }
        Map<String, Product> productMap = productRepository.findBySkus(skus);

        for (String sku : skus) {
            if (!productMap.containsKey(sku)) {
                throw new ApiException("UNKNOWN_PRODUCT", 422);
            }
        }

        for (OrderItem item : request.getItems()) {
            Product product = productMap.get(item.getSku());
            if (product.isDiscontinued()) {
                throw new ApiException("PRODUCT_DISCONTINUED", 422);
            }
            if (product.getStock() < item.getQuantity()) {
                throw new ApiException("INSUFFICIENT_STOCK", 422);
            }
        }

        if (request.getClubTier() == null) {
            throw new ApiException("INVALID_CLUB_TIER", 422);
        }

        if (request.getRegion() == null) {
            throw new ApiException("INVALID_REGION", 422);
        }

        if (request.getZipCode() == null || !request.getZipCode().matches("\\d{8}")) {
            throw new ApiException("INVALID_ZIP_CODE", 422);
        }

        if (request.getShippingMethod() == null) {
            throw new ApiException("INVALID_SHIPPING_METHOD", 422);
        }

        if (request.getCoupon() != null && !request.getCoupon().isEmpty() && !couponService.exists(request.getCoupon())) {
            throw new ApiException("INVALID_COUPON", 422);
        }

        if (request.getPaymentMethod() == null) {
            throw new ApiException("INVALID_PAYMENT_METHOD", 422);
        }

        int installments = request.getInstallments() != null ? request.getInstallments() : 1;
        if (request.getPaymentMethod() == PaymentMethod.PIX || request.getPaymentMethod() == PaymentMethod.BANK_SLIP) {
            if (installments != 1) {
                throw new ApiException("INVALID_INSTALLMENTS", 422);
            }
        } else if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            if (installments < 1 || installments > 12) {
                throw new ApiException("INVALID_INSTALLMENTS", 422);
            }
        }

        BigDecimal productsSubtotal = calculateProductsSubtotal(request.getItems(), productMap);
        productsSubtotal = MoneyUtils.roundMoney(productsSubtotal);

        boolean isFirstPurchase = conn == null || !orderRepository.hasCompletedOrders(request.getCustomer().getCpf(), conn);

        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request.getCoupon() != null && !request.getCoupon().isEmpty()) {
            CouponStrategy coupon = couponService.getCoupon(request.getCoupon());
            if (!coupon.isApplicable(request, productMap, isFirstPurchase)) {
                return;
            }

            ShippingResult shippingForCoupon = shippingCalculator.calculate(request, productMap);
            BigDecimal preliminaryShipping = clubBenefitsCalculator.hasFreeShipping(request.getClubTier(), request.getShippingMethod())
                ? BigDecimal.ZERO
                : shippingForCoupon.getCost();

            couponDiscount = coupon.calculateDiscount(productsSubtotal, preliminaryShipping, request.getItems(), productMap);
            couponDiscount = couponService.applyCap(couponDiscount, productsSubtotal);
        }

        ShippingResult shippingResult = shippingCalculator.calculate(request, productMap);
        BigDecimal shippingCost = clubBenefitsCalculator.hasFreeShipping(request.getClubTier(), request.getShippingMethod())
            ? BigDecimal.ZERO
            : shippingResult.getCost();

        BigDecimal insurance = insuranceCalculator.calculate(productsSubtotal, request.getRegion());

        BigDecimal orderTotal = productsSubtotal.subtract(couponDiscount).add(shippingCost).add(insurance);

        BigDecimal paymentAdjustment = paymentAdjustmentCalculator.calculate(orderTotal, request.getPaymentMethod(), installments, request.getCoupon() != null && !request.getCoupon().isEmpty());
        BigDecimal finalTotal = orderTotal.add(paymentAdjustment);

        if (request.getPaymentMethod() == PaymentMethod.BANK_SLIP && finalTotal.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new ApiException("PAYMENT_METHOD_UNAVAILABLE", 422);
        }
    }

    private BigDecimal calculateProductsSubtotal(List<OrderItem> items, Map<String, Product> productMap) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Product product = productMap.get(item.getSku());
            total = total.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        return total;
    }
}
