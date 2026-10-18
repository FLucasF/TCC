package com.example.application.service;

import com.example.api.dto.CheckoutRequestDto;
import com.example.domain.enums.CouponCode;
import com.example.domain.enums.ShippingMethod;
import com.example.domain.entity.Order;
import com.example.domain.repository.OrderRepository;
import com.example.application.exception.CouponNotApplicableException;
import java.math.BigDecimal;
import java.util.*;

public class CouponValidator {

    private final OrderRepository orderRepository;

    public CouponValidator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void validate(CheckoutRequestDto request, BigDecimal productsSubtotal) {
        if (request.getCoupon() == null || request.getCoupon().isEmpty()) {
            return;
        }

        CouponCode coupon = CouponCode.valueOf(request.getCoupon());

        switch (coupon) {
            case WELCOME10:
                validateWelcome10(request);
                break;
            case MINUS50:
                validateMinus50(productsSubtotal);
                break;
            case FREESHIPPING:
                validateFreeShipping(request);
                break;
            case BUY3PAY2:
                break;
        }
    }

    private void validateWelcome10(CheckoutRequestDto request) {
        String cpf = request.getCustomer().getCpf();
        List<Order> customerOrders = orderRepository.findAll();
        boolean hasPaidOrder = customerOrders.stream()
                .anyMatch(o -> o.getCustomerCpf().equals(cpf) &&
                        (o.getStatus().name().equals("PAID") ||
                         o.getStatus().name().equals("SHIPPED") ||
                         o.getStatus().name().equals("DELIVERED")));

        if (hasPaidOrder) {
            throw new CouponNotApplicableException();
        }
    }

    private void validateMinus50(BigDecimal productsSubtotal) {
        if (productsSubtotal.compareTo(new BigDecimal("300")) < 0) {
            throw new CouponNotApplicableException();
        }
    }

    private void validateFreeShipping(CheckoutRequestDto request) {
        ShippingMethod method = ShippingMethod.valueOf(request.getShippingMethod());
        if (method == ShippingMethod.STORE_PICKUP) {
            throw new CouponNotApplicableException();
        }
    }
}
