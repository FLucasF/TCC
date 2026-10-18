package com.loja.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.orderservice.domain.entity.Order;
import com.loja.orderservice.domain.entity.Product;
import com.loja.orderservice.domain.enums.*;
import com.loja.orderservice.dto.CheckoutSummary;
import com.loja.orderservice.dto.CheckoutSummaryRequest;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.integration.TransLogClient;
import com.loja.orderservice.repository.ProductRepository;
import com.loja.orderservice.util.CurrencyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    private static final int MAX_COURIER_WEIGHT_KG = 5;
    private static final BigDecimal COURIER_COST = new BigDecimal("18.00");
    private static final BigDecimal BANK_SLIP_FEE = new BigDecimal("3.49");
    private static final BigDecimal PIX_DISCOUNT_PERCENT = new BigDecimal("0.05");
    private static final BigDecimal COUPON_MAX_PERCENT = new BigDecimal("0.30");
    private static final int NORTH_EXTRA_DAYS = 3;
    private static final int NORTHEAST_EXTRA_DAYS = 2;
    private static final int STORE_PICKUP_DAYS = 1;
    private static final int COURIER_DELIVERY_DAYS = 0;

    private final ProductRepository productRepository;
    private final TransLogClient transLogClient;
    private final ObjectMapper objectMapper;

    public CheckoutSummary calculateSummary(CheckoutSummaryRequest request) {
        validateRequest(request);

        List<Product> products = getAndValidateProducts(request.getItems());
        validateCoupon(request.getCoupon(), request.getItems(), products);

        BigDecimal productsSubtotal = calculateProductsSubtotal(request.getItems(), products);
        BigDecimal couponDiscount = calculateCouponDiscount(request.getCoupon(), request.getItems(),
                products, productsSubtotal, request.getCustomer().getCpf());

        BigDecimal shippingCost = BigDecimal.ZERO;
        Integer deliveryDays = 0;
        if (request.getShippingMethod() != ShippingMethod.STORE_PICKUP &&
            request.getClubTier() != ClubTier.GOLD) {
            ShippingInfo shippingInfo = calculateShipping(request.getShippingMethod(),
                    request.getZipCode(), request.getItems(), products, request.getRegion());
            shippingCost = shippingInfo.cost;
            deliveryDays = shippingInfo.days;
        } else if (request.getShippingMethod() != ShippingMethod.STORE_PICKUP &&
                   request.getClubTier() == ClubTier.GOLD) {
            ShippingInfo shippingInfo = calculateShipping(request.getShippingMethod(),
                    request.getZipCode(), request.getItems(), products, request.getRegion());
            deliveryDays = shippingInfo.days;
        } else if (request.getShippingMethod() == ShippingMethod.STORE_PICKUP) {
            deliveryDays = STORE_PICKUP_DAYS;
        }

        BigDecimal insurance = calculateInsurance(productsSubtotal, request.getRegion());

        BigDecimal subtotalBeforePayment = CurrencyUtils.add(productsSubtotal,
                CurrencyUtils.subtract(BigDecimal.ZERO, couponDiscount), shippingCost, insurance);

        BigDecimal paymentAdjustment = calculatePaymentAdjustment(subtotalBeforePayment,
                request.getPaymentMethod(), request.getInstallments());

        BigDecimal finalTotal = CurrencyUtils.add(subtotalBeforePayment, paymentAdjustment);

        Integer installments = request.getInstallments() != null ? request.getInstallments() : 1;
        BigDecimal installmentAmount = calculateInstallmentAmount(finalTotal, installments);

        BigDecimal nextPurchaseCredit = calculateNextPurchaseCredit(request.getClubTier(), productsSubtotal);

        Boolean gift = request.getClubTier() == ClubTier.GOLD &&
                      productsSubtotal.compareTo(new BigDecimal("500.00")) > 0;

        return CheckoutSummary.builder()
                .productsSubtotal(productsSubtotal)
                .couponDiscount(couponDiscount)
                .shippingCost(shippingCost)
                .deliveryDays(deliveryDays)
                .insurance(insurance)
                .paymentAdjustment(paymentAdjustment)
                .finalTotal(finalTotal)
                .installments(installments)
                .installmentAmount(installmentAmount)
                .nextPurchaseCredit(nextPurchaseCredit)
                .gift(gift)
                .build();
    }

    private void validateRequest(CheckoutSummaryRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderServiceException("INVALID_ORDER", "Cart is empty", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Set<String> skus = new HashSet<>();
        for (var item : request.getItems()) {
            if (item.getSku() == null || item.getSku().isEmpty()) {
                throw new OrderServiceException("INVALID_ORDER", "Item SKU is missing", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderServiceException("INVALID_ORDER", "Item quantity must be positive", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!skus.add(item.getSku())) {
                throw new OrderServiceException("INVALID_ORDER", "Duplicate SKU in cart", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        if (request.getCustomer() == null || request.getCustomer().getCpf() == null ||
            request.getCustomer().getCpf().isEmpty() || request.getCustomer().getEmail() == null ||
            request.getCustomer().getEmail().isEmpty()) {
            throw new OrderServiceException("INVALID_ORDER", "Customer CPF and email are required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getClubTier() == null) {
            throw new OrderServiceException("INVALID_CLUB_TIER", "Club tier is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getRegion() == null) {
            throw new OrderServiceException("INVALID_REGION", "Region is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getZipCode() == null || request.getZipCode().length() != 8 ||
            !request.getZipCode().matches("\\d{8}")) {
            throw new OrderServiceException("INVALID_ZIP_CODE", "Invalid ZIP code", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getShippingMethod() == null) {
            throw new OrderServiceException("INVALID_SHIPPING_METHOD", "Shipping method is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getPaymentMethod() == null) {
            throw new OrderServiceException("INVALID_PAYMENT_METHOD", "Payment method is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Integer installments = request.getInstallments() != null ? request.getInstallments() : 1;
        if (installments < 1 || installments > 12) {
            throw new OrderServiceException("INVALID_INSTALLMENTS", "Invalid number of installments", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getPaymentMethod() == PaymentMethod.PIX || request.getPaymentMethod() == PaymentMethod.BANK_SLIP) {
            if (installments != 1) {
                throw new OrderServiceException("INVALID_INSTALLMENTS", "PIX and BANK_SLIP do not support installments", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private List<Product> getAndValidateProducts(List<CheckoutSummaryRequest.OrderItemRequest> items) {
        List<Product> products = new ArrayList<>();

        for (var item : items) {
            Optional<Product> product = productRepository.findById(item.getSku());

            if (product.isEmpty()) {
                throw new OrderServiceException("UNKNOWN_PRODUCT", "Product not found: " + item.getSku(), HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (product.get().getDiscontinued()) {
                throw new OrderServiceException("PRODUCT_DISCONTINUED", "Product is discontinued: " + item.getSku(), HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (product.get().getStock() < item.getQuantity()) {
                throw new OrderServiceException("INSUFFICIENT_STOCK", "Insufficient stock for: " + item.getSku(), HttpStatus.UNPROCESSABLE_ENTITY);
            }

            products.add(product.get());
        }

        return products;
    }

    private BigDecimal calculateProductsSubtotal(List<CheckoutSummaryRequest.OrderItemRequest> items,
                                                  List<Product> products) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            BigDecimal itemTotal = CurrencyUtils.multiply(
                    products.get(i).getPrice(),
                    new BigDecimal(items.get(i).getQuantity())
            );
            subtotal = CurrencyUtils.add(subtotal, itemTotal);
        }

        return subtotal;
    }

    private void validateCoupon(String coupon, List<CheckoutSummaryRequest.OrderItemRequest> items,
                                List<Product> products) {
        if (coupon == null || coupon.isEmpty()) {
            return;
        }

        if (!isValidCoupon(coupon)) {
            throw new OrderServiceException("INVALID_COUPON", "Coupon not found: " + coupon, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private boolean isValidCoupon(String coupon) {
        return "WELCOME10".equals(coupon) || "MINUS50".equals(coupon) ||
               "FREESHIPPING".equals(coupon) || "BUY3PAY2".equals(coupon);
    }

    private BigDecimal calculateCouponDiscount(String coupon,
                                               List<CheckoutSummaryRequest.OrderItemRequest> items,
                                               List<Product> products, BigDecimal productsSubtotal,
                                               String cpf) {
        if (coupon == null || coupon.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        if ("WELCOME10".equals(coupon)) {
            discount = CurrencyUtils.multiply(productsSubtotal, new BigDecimal("0.10"));
        } else if ("MINUS50".equals(coupon)) {
            if (productsSubtotal.compareTo(new BigDecimal("300.00")) < 0) {
                throw new OrderServiceException("COUPON_NOT_APPLICABLE", "Coupon not applicable", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            discount = new BigDecimal("50.00");
        } else if ("FREESHIPPING".equals(coupon)) {
            throw new OrderServiceException("COUPON_NOT_APPLICABLE", "FREESHIPPING can only be applied during shipping calculation", HttpStatus.UNPROCESSABLE_ENTITY);
        } else if ("BUY3PAY2".equals(coupon)) {
            discount = calculateBuy3Pay2Discount(items, products);
        }

        BigDecimal maxDiscount = CurrencyUtils.multiply(productsSubtotal, COUPON_MAX_PERCENT);
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    private BigDecimal calculateBuy3Pay2Discount(List<CheckoutSummaryRequest.OrderItemRequest> items,
                                                 List<Product> products) {
        BigDecimal discount = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            long quantity = items.get(i).getQuantity();
            long freeItems = quantity / 3;
            if (freeItems > 0) {
                BigDecimal itemDiscount = CurrencyUtils.multiply(
                        products.get(i).getPrice(),
                        new BigDecimal(freeItems)
                );
                discount = CurrencyUtils.add(discount, itemDiscount);
            }
        }

        return discount;
    }

    private ShippingInfo calculateShipping(ShippingMethod method, String zipCode,
                                          List<CheckoutSummaryRequest.OrderItemRequest> items,
                                          List<Product> products, Region region) {
        BigDecimal totalWeight = calculateTotalWeight(items, products);

        if (method == ShippingMethod.COURIER) {
            if (totalWeight.compareTo(new BigDecimal(MAX_COURIER_WEIGHT_KG)) > 0) {
                throw new OrderServiceException("SHIPPING_METHOD_UNAVAILABLE", "Courier does not support this weight", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return new ShippingInfo(COURIER_COST, COURIER_DELIVERY_DAYS);
        } else if (method == ShippingMethod.STORE_PICKUP) {
            return new ShippingInfo(BigDecimal.ZERO, STORE_PICKUP_DAYS);
        } else if (method == ShippingMethod.ECONOMY || method == ShippingMethod.EXPRESS) {
            String service = method == ShippingMethod.ECONOMY ? "STD" : "EXP";
            int weightGrams = totalWeight.multiply(new BigDecimal("1000"))
                    .setScale(0, RoundingMode.DOWN).intValue();

            Optional<TransLogClient.TransLogQuoteResponse> quote = transLogClient.getQuote(zipCode, weightGrams, service);

            if (quote.isEmpty()) {
                throw new OrderServiceException("SHIPPING_METHOD_UNAVAILABLE",
                        "Shipping method not available for this location", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            TransLogClient.TransLogQuoteResponse response = quote.get();
            BigDecimal cost = new BigDecimal(response.getPriceCents()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
            int days = (int) Math.ceil(response.getEtaHours() / 24.0);

            if (region == Region.NORTH) {
                days += NORTH_EXTRA_DAYS;
            } else if (region == Region.NORTHEAST) {
                days += NORTHEAST_EXTRA_DAYS;
            }

            return new ShippingInfo(cost, days);
        }

        throw new OrderServiceException("INVALID_SHIPPING_METHOD", "Invalid shipping method", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private BigDecimal calculateTotalWeight(List<CheckoutSummaryRequest.OrderItemRequest> items,
                                           List<Product> products) {
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            BigDecimal itemWeight = products.get(i).getWeightKg()
                    .multiply(new BigDecimal(items.get(i).getQuantity()));
            totalWeight = totalWeight.add(itemWeight);
        }

        return totalWeight;
    }

    private BigDecimal calculateInsurance(BigDecimal productsSubtotal, Region region) {
        BigDecimal percentage;
        BigDecimal limit = null;

        switch (region) {
            case SOUTHEAST, SOUTH -> percentage = new BigDecimal("0.01");
            case MIDWEST -> percentage = new BigDecimal("0.015");
            case NORTH -> {
                percentage = new BigDecimal("0.025");
                limit = new BigDecimal("30.00");
            }
            case NORTHEAST -> {
                percentage = new BigDecimal("0.02");
                limit = new BigDecimal("30.00");
            }
            default -> percentage = BigDecimal.ZERO;
        }

        BigDecimal insurance = CurrencyUtils.multiply(productsSubtotal, percentage);

        if (limit != null && insurance.compareTo(limit) > 0) {
            insurance = limit;
        }

        return insurance;
    }

    private BigDecimal calculatePaymentAdjustment(BigDecimal subtotal, PaymentMethod method, Integer installments) {
        int numInstallments = installments != null ? installments : 1;

        if (method == PaymentMethod.PIX) {
            return CurrencyUtils.multiply(subtotal, PIX_DISCOUNT_PERCENT.negate());
        } else if (method == PaymentMethod.BANK_SLIP) {
            return BANK_SLIP_FEE;
        } else if (method == PaymentMethod.CREDIT_CARD) {
            if (numInstallments <= 3) {
                return BigDecimal.ZERO;
            }
            BigDecimal monthlyRate = new BigDecimal("0.0199");
            BigDecimal numerator = monthlyRate;
            BigDecimal denominator = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.add(monthlyRate).pow(-numInstallments)
            );
            BigDecimal installmentValue = subtotal.multiply(numerator.divide(denominator, 10, RoundingMode.HALF_EVEN));
            installmentValue = CurrencyUtils.roundToCents(installmentValue);
            BigDecimal totalValue = installmentValue.multiply(new BigDecimal(numInstallments));
            return CurrencyUtils.subtract(totalValue, subtotal);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calculateInstallmentAmount(BigDecimal total, Integer installments) {
        int numInstallments = installments != null && installments > 0 ? installments : 1;
        return CurrencyUtils.divide(total, new BigDecimal(numInstallments));
    }

    private BigDecimal calculateNextPurchaseCredit(ClubTier tier, BigDecimal productsSubtotal) {
        if (tier == ClubTier.BRONZE) {
            return BigDecimal.ZERO;
        } else if (tier == ClubTier.SILVER) {
            return CurrencyUtils.multiply(productsSubtotal, new BigDecimal("0.02"));
        } else if (tier == ClubTier.GOLD) {
            return CurrencyUtils.multiply(productsSubtotal, new BigDecimal("0.05"));
        }

        return BigDecimal.ZERO;
    }

    private static class ShippingInfo {
        BigDecimal cost;
        Integer days;

        ShippingInfo(BigDecimal cost, Integer days) {
            this.cost = cost;
            this.days = days;
        }
    }
}
