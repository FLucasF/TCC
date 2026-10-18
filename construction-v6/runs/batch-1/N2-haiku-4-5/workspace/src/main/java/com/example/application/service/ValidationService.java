package com.example.application.service;

import com.example.api.dto.CheckoutRequestDto;
import com.example.domain.entity.Order;
import com.example.domain.entity.Product;
import com.example.domain.enums.*;
import com.example.application.exception.*;
import java.math.BigDecimal;
import java.util.*;

public class ValidationService {

    private final ProductService productService;

    public ValidationService(ProductService productService) {
        this.productService = productService;
    }

    public void validateCheckout(CheckoutRequestDto request) {
        validateItems(request);
        validateCustomer(request);
        validateRegion(request);
        validateZipCode(request);
        validateShippingMethod(request);
        validateClubTier(request);
        validateCoupon(request);
        validatePaymentMethod(request);
    }

    private void validateItems(CheckoutRequestDto request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOrderException();
        }

        Set<String> seenSkus = new HashSet<>();
        for (var item : request.getItems()) {
            if (item.getSku() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InvalidOrderException();
            }
            if (!seenSkus.add(item.getSku())) {
                throw new InvalidOrderException();
            }
        }

        for (var item : request.getItems()) {
            Product product = productService.getProduct(item.getSku());
            if (product == null) {
                throw new UnknownProductException();
            }
            if (product.isDiscontinued()) {
                throw new ProductDiscontinuedException();
            }
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException();
            }
        }
    }

    private void validateCustomer(CheckoutRequestDto request) {
        if (request.getCustomer() == null ||
                request.getCustomer().getCpf() == null ||
                request.getCustomer().getEmail() == null) {
            throw new InvalidOrderException();
        }
    }

    private void validateRegion(CheckoutRequestDto request) {
        try {
            Region.valueOf(request.getRegion());
        } catch (IllegalArgumentException e) {
            throw new InvalidRegionException();
        }
    }

    private void validateZipCode(CheckoutRequestDto request) {
        if (request.getZipCode() == null || !request.getZipCode().matches("\\d{8}")) {
            throw new InvalidZipCodeException();
        }
    }

    private void validateShippingMethod(CheckoutRequestDto request) {
        ShippingMethod method;
        try {
            method = ShippingMethod.valueOf(request.getShippingMethod());
        } catch (IllegalArgumentException e) {
            throw new InvalidShippingMethodException();
        }

        if (method == ShippingMethod.COURIER) {
            BigDecimal totalWeight = calculateTotalWeight(request);
            if (totalWeight.compareTo(new BigDecimal("5")) > 0) {
                throw new ShippingMethodUnavailableException();
            }
        }
    }

    private void validateClubTier(CheckoutRequestDto request) {
        try {
            ClubTier.valueOf(request.getClubTier());
        } catch (IllegalArgumentException e) {
            throw new InvalidClubTierException();
        }
    }

    private void validateCoupon(CheckoutRequestDto request) {
        if (request.getCoupon() != null && !request.getCoupon().isEmpty()) {
            try {
                CouponCode.valueOf(request.getCoupon());
            } catch (IllegalArgumentException e) {
                throw new InvalidCouponException();
            }
        }
    }

    private void validatePaymentMethod(CheckoutRequestDto request) {
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentMethodException();
        }

        Integer installments = request.getInstallments() != null ? request.getInstallments() : 1;

        if (method == PaymentMethod.PIX || method == PaymentMethod.BANK_SLIP) {
            if (installments != 1) {
                throw new InvalidInstallmentsException();
            }
        } else if (method == PaymentMethod.CREDIT_CARD) {
            if (installments < 1 || installments > 12) {
                throw new InvalidInstallmentsException();
            }
        }
    }

    private BigDecimal calculateTotalWeight(CheckoutRequestDto request) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            Product product = productService.getProduct(item.getSku());
            BigDecimal itemWeight = product.getWeightKg().multiply(new BigDecimal(item.getQuantity()));
            totalWeight = totalWeight.add(itemWeight);
        }
        return totalWeight;
    }
}
