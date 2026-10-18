package com.loja.orderservice.service;

import com.loja.orderservice.dto.CheckoutRequest;
import com.loja.orderservice.entity.OrderSummary;
import com.loja.orderservice.entity.Product;
import com.loja.orderservice.exception.BusinessException;
import com.loja.orderservice.model.*;
import com.loja.orderservice.repository.OrderRepository;
import com.loja.orderservice.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CheckoutCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final TransLogService transLogService;

    public CheckoutCalculator(ProductRepository productRepository,
                              OrderRepository orderRepository,
                              TransLogService transLogService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.transLogService = transLogService;
    }

    public OrderSummary calculate(CheckoutRequest request) {
        validateRequest(request);

        Map<String, Product> productMap = loadProducts(request);
        Map<String, Integer> itemQuantities = getItemQuantities(request);

        validateProducts(productMap, itemQuantities);

        BigDecimal productsSubtotal = calculateProductsSubtotal(productMap, itemQuantities);

        ShippingInfo shippingInfo = calculateShipping(request, productMap, itemQuantities);

        BigDecimal shippingCost = shippingInfo.cost;
        if (request.getClubTier() == ClubTier.GOLD && request.getShippingMethod() != ShippingMethod.COURIER) {
            shippingCost = ZERO;
        }

        BigDecimal couponDiscount = calculateCouponDiscount(
                request, productsSubtotal, shippingCost, itemQuantities);

        BigDecimal insurance = calculateInsurance(request.getRegion(), productsSubtotal);

        BigDecimal orderTotal = productsSubtotal
                .subtract(couponDiscount)
                .add(shippingCost)
                .add(insurance);

        PaymentInfo paymentInfo = calculatePayment(request, orderTotal);

        BigDecimal nextPurchaseCredit = calculateClubCredit(request.getClubTier(), productsSubtotal);

        boolean gift = shouldGiveGift(request.getClubTier(), request.getShippingMethod(), productsSubtotal);

        OrderSummary summary = new OrderSummary();
        summary.setProductsSubtotal(productsSubtotal);
        summary.setCouponDiscount(couponDiscount);
        summary.setShippingCost(shippingCost);
        summary.setDeliveryDays(shippingInfo.days);
        summary.setInsurance(insurance);
        summary.setPaymentAdjustment(paymentInfo.adjustment);
        summary.setFinalTotal(paymentInfo.finalTotal);
        summary.setInstallments(paymentInfo.installments);
        summary.setInstallmentAmount(paymentInfo.installmentAmount);
        summary.setNextPurchaseCredit(nextPurchaseCredit);
        summary.setGift(gift);

        return summary;
    }

    private void validateRequest(CheckoutRequest request) {
        if (request.getCustomer() == null ||
            request.getCustomer().getCpf() == null ||
            request.getCustomer().getEmail() == null) {
            throw new BusinessException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Set<String> skus = new HashSet<>();
        for (var item : request.getItems()) {
            if (item.getSku() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!skus.add(item.getSku())) {
                throw new BusinessException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        if (request.getClubTier() == null) {
            throw new BusinessException("INVALID_CLUB_TIER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getRegion() == null) {
            throw new BusinessException("INVALID_REGION", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getZipCode() == null || !request.getZipCode().matches("\\d{8}")) {
            throw new BusinessException("INVALID_ZIP_CODE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getShippingMethod() == null) {
            throw new BusinessException("INVALID_SHIPPING_METHOD", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getCoupon() != null) {
            try {
                Coupon.valueOf(request.getCoupon().name());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_COUPON", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        if (request.getPaymentMethod() == null) {
            throw new BusinessException("INVALID_PAYMENT_METHOD", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        int installments = request.getInstallments() != null ? request.getInstallments() : 1;
        validateInstallments(request.getPaymentMethod(), installments);
    }

    private void validateInstallments(PaymentMethod paymentMethod, int installments) {
        switch (paymentMethod) {
            case PIX, BANK_SLIP -> {
                if (installments != 1) {
                    throw new BusinessException("INVALID_INSTALLMENTS", HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
            case CREDIT_CARD -> {
                if (installments < 1 || installments > 12) {
                    throw new BusinessException("INVALID_INSTALLMENTS", HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
        }
    }

    private Map<String, Product> loadProducts(CheckoutRequest request) {
        List<String> skus = request.getItems().stream()
                .map(CheckoutRequest.ItemRequest::getSku)
                .toList();

        List<Product> products = productRepository.findAllById(skus);

        if (products.size() != skus.size()) {
            throw new BusinessException("UNKNOWN_PRODUCT", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map<String, Product> map = new HashMap<>();
        for (Product p : products) {
            map.put(p.getSku(), p);
        }
        return map;
    }

    private Map<String, Integer> getItemQuantities(CheckoutRequest request) {
        Map<String, Integer> map = new HashMap<>();
        for (var item : request.getItems()) {
            map.put(item.getSku(), item.getQuantity());
        }
        return map;
    }

    private void validateProducts(Map<String, Product> productMap, Map<String, Integer> itemQuantities) {
        for (var entry : itemQuantities.entrySet()) {
            Product product = productMap.get(entry.getKey());

            if (product.getDiscontinued()) {
                throw new BusinessException("PRODUCT_DISCONTINUED", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (product.getStock() < entry.getValue()) {
                throw new BusinessException("INSUFFICIENT_STOCK", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private BigDecimal calculateProductsSubtotal(Map<String, Product> productMap,
                                                  Map<String, Integer> itemQuantities) {
        BigDecimal total = ZERO;
        for (var entry : itemQuantities.entrySet()) {
            Product product = productMap.get(entry.getKey());
            BigDecimal itemTotal = product.getPrice()
                    .multiply(new BigDecimal(entry.getValue()))
                    .setScale(2, RoundingMode.HALF_EVEN);
            total = total.add(itemTotal);
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    private ShippingInfo calculateShipping(CheckoutRequest request,
                                           Map<String, Product> productMap,
                                           Map<String, Integer> itemQuantities) {
        BigDecimal totalWeight = ZERO;
        for (var entry : itemQuantities.entrySet()) {
            Product product = productMap.get(entry.getKey());
            BigDecimal itemWeight = product.getWeightKg().multiply(new BigDecimal(entry.getValue()));
            totalWeight = totalWeight.add(itemWeight);
        }

        switch (request.getShippingMethod()) {
            case STORE_PICKUP -> {
                return new ShippingInfo(ZERO, 1);
            }
            case COURIER -> {
                if (totalWeight.compareTo(new BigDecimal("5.0")) > 0) {
                    throw new BusinessException("SHIPPING_METHOD_UNAVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                return new ShippingInfo(new BigDecimal("18.00"), 0);
            }
            case ECONOMY, EXPRESS -> {
                String service = request.getShippingMethod() == ShippingMethod.ECONOMY ? "STD" : "EXP";

                TransLogService.QuoteResult quote = transLogService.getQuote(
                        request.getZipCode(), totalWeight, service);

                if (!quote.isAvailable()) {
                    throw new BusinessException("CARRIER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (!quote.isServed()) {
                    throw new BusinessException("SHIPPING_METHOD_UNAVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY);
                }

                int extraDays = getExtraDays(request.getRegion());
                return new ShippingInfo(quote.getPrice(), quote.getDays() + extraDays);
            }
            default -> throw new BusinessException("INVALID_SHIPPING_METHOD", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private int getExtraDays(Region region) {
        return switch (region) {
            case NORTH -> 3;
            case NORTHEAST -> 2;
            default -> 0;
        };
    }

    private BigDecimal calculateCouponDiscount(CheckoutRequest request,
                                                BigDecimal productsSubtotal,
                                                BigDecimal shippingCost,
                                                Map<String, Integer> itemQuantities) {
        if (request.getCoupon() == null) {
            return ZERO;
        }

        BigDecimal discount = switch (request.getCoupon()) {
            case WELCOME10 -> {
                if (!isFirstPurchase(request.getCustomer().getCpf())) {
                    throw new BusinessException("COUPON_NOT_APPLICABLE", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                yield productsSubtotal.multiply(new BigDecimal("0.10"));
            }
            case MINUS50 -> {
                if (productsSubtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    throw new BusinessException("COUPON_NOT_APPLICABLE", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                yield new BigDecimal("50.00");
            }
            case FREESHIPPING -> {
                if (request.getShippingMethod() == ShippingMethod.STORE_PICKUP) {
                    throw new BusinessException("COUPON_NOT_APPLICABLE", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                yield shippingCost;
            }
            case BUY3PAY2 -> {
                BigDecimal buy3pay2Discount = ZERO;
                for (var entry : itemQuantities.entrySet()) {
                    if (entry.getValue() >= 3) {
                        Product product = productRepository.findById(entry.getKey()).orElseThrow();
                        int freeItems = entry.getValue() / 3;
                        buy3pay2Discount = buy3pay2Discount.add(
                                product.getPrice().multiply(new BigDecimal(freeItems)));
                    }
                }
                yield buy3pay2Discount;
            }
        };

        discount = discount.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal maxDiscount = productsSubtotal.multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_EVEN);

        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    private boolean isFirstPurchase(String cpf) {
        long paidOrders = orderRepository.countByCustomerCpfAndStatusIn(
                cpf, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
        return paidOrders == 0;
    }

    private BigDecimal calculateInsurance(Region region, BigDecimal productsSubtotal) {
        BigDecimal rate;
        BigDecimal limit;

        switch (region) {
            case SOUTHEAST, SOUTH -> {
                rate = new BigDecimal("0.01");
                limit = null;
            }
            case MIDWEST -> {
                rate = new BigDecimal("0.015");
                limit = null;
            }
            case NORTH -> {
                rate = new BigDecimal("0.025");
                limit = new BigDecimal("30.00");
            }
            case NORTHEAST -> {
                rate = new BigDecimal("0.02");
                limit = new BigDecimal("30.00");
            }
            default -> {
                rate = ZERO;
                limit = null;
            }
        }

        BigDecimal insurance = productsSubtotal.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);

        if (limit != null && insurance.compareTo(limit) > 0) {
            insurance = limit;
        }

        return insurance;
    }

    private PaymentInfo calculatePayment(CheckoutRequest request, BigDecimal orderTotal) {
        int installments = request.getInstallments() != null ? request.getInstallments() : 1;

        BigDecimal adjustment;
        BigDecimal finalTotal;
        BigDecimal installmentAmount;

        switch (request.getPaymentMethod()) {
            case PIX -> {
                if (request.getCoupon() == null) {
                    BigDecimal discount = orderTotal.multiply(new BigDecimal("0.05"))
                            .setScale(2, RoundingMode.HALF_EVEN);
                    adjustment = discount.negate();
                    finalTotal = orderTotal.subtract(discount);
                } else {
                    adjustment = ZERO;
                    finalTotal = orderTotal;
                }
                installmentAmount = finalTotal;
            }
            case BANK_SLIP -> {
                if (orderTotal.compareTo(new BigDecimal("1000.00")) > 0) {
                    throw new BusinessException("PAYMENT_METHOD_UNAVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                BigDecimal fee = new BigDecimal("3.49");
                adjustment = fee;
                finalTotal = orderTotal.add(fee);
                installmentAmount = finalTotal;
            }
            case CREDIT_CARD -> {
                if (installments <= 3) {
                    adjustment = ZERO;
                    finalTotal = orderTotal;
                    installmentAmount = finalTotal.divide(new BigDecimal(installments), 2, RoundingMode.HALF_EVEN);
                } else {
                    BigDecimal monthlyRate = new BigDecimal("0.0199");
                    BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
                    BigDecimal factor = onePlusRate.pow(installments);

                    installmentAmount = orderTotal.multiply(monthlyRate)
                            .divide(BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor, 10, RoundingMode.HALF_EVEN)),
                                    2, RoundingMode.HALF_EVEN);

                    finalTotal = installmentAmount.multiply(new BigDecimal(installments));
                    adjustment = finalTotal.subtract(orderTotal);
                }
            }
            default -> throw new BusinessException("INVALID_PAYMENT_METHOD", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return new PaymentInfo(adjustment, finalTotal, installments, installmentAmount);
    }

    private BigDecimal calculateClubCredit(ClubTier tier, BigDecimal productsSubtotal) {
        BigDecimal rate = switch (tier) {
            case BRONZE -> ZERO;
            case SILVER -> new BigDecimal("0.02");
            case GOLD -> new BigDecimal("0.05");
        };

        return productsSubtotal.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean shouldGiveGift(ClubTier tier, ShippingMethod shippingMethod, BigDecimal productsSubtotal) {
        if (tier != ClubTier.GOLD) {
            return false;
        }

        if (shippingMethod == ShippingMethod.COURIER) {
            return false;
        }

        return productsSubtotal.compareTo(new BigDecimal("500.00")) > 0;
    }

    public OrderSummary applyGoldBenefits(OrderSummary summary, ClubTier tier, ShippingMethod shippingMethod) {
        if (tier == ClubTier.GOLD && shippingMethod != ShippingMethod.COURIER) {
            summary.setShippingCost(ZERO);
        }
        return summary;
    }

    private static class ShippingInfo {
        final BigDecimal cost;
        final int days;

        ShippingInfo(BigDecimal cost, int days) {
            this.cost = cost;
            this.days = days;
        }
    }

    private static class PaymentInfo {
        final BigDecimal adjustment;
        final BigDecimal finalTotal;
        final int installments;
        final BigDecimal installmentAmount;

        PaymentInfo(BigDecimal adjustment, BigDecimal finalTotal, int installments, BigDecimal installmentAmount) {
            this.adjustment = adjustment;
            this.finalTotal = finalTotal;
            this.installments = installments;
            this.installmentAmount = installmentAmount;
        }
    }
}
