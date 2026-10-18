package com.shop.service;

import com.shop.model.enums.CouponCode;
import com.shop.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentService {

    public static class PaymentCalculation {
        public BigDecimal adjustment;
        public BigDecimal finalTotal;
        public int installments;
        public BigDecimal installmentAmount;

        public PaymentCalculation(BigDecimal adjustment, BigDecimal finalTotal,
                                 int installments, BigDecimal installmentAmount) {
            this.adjustment = adjustment;
            this.finalTotal = finalTotal;
            this.installments = installments;
            this.installmentAmount = installmentAmount;
        }
    }

    public PaymentCalculation calculatePayment(PaymentMethod method, BigDecimal orderTotal,
                                              int installments, CouponCode coupon,
                                              BigDecimal couponDiscount) {
        return switch (method) {
            case PIX -> calculatePix(orderTotal, coupon, couponDiscount);
            case BANK_SLIP -> calculateBankSlip(orderTotal);
            case CREDIT_CARD -> calculateCreditCard(orderTotal, installments);
        };
    }

    private PaymentCalculation calculatePix(BigDecimal orderTotal, CouponCode coupon, BigDecimal couponDiscount) {
        if (coupon != null && couponDiscount.compareTo(BigDecimal.ZERO) > 0) {
            return new PaymentCalculation(BigDecimal.ZERO, orderTotal, 1, orderTotal);
        }

        BigDecimal discount = orderTotal.multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal finalTotal = orderTotal.subtract(discount);

        return new PaymentCalculation(discount.negate(), finalTotal, 1, finalTotal);
    }

    private PaymentCalculation calculateBankSlip(BigDecimal orderTotal) {
        BigDecimal fee = new BigDecimal("3.49");
        BigDecimal finalTotal = orderTotal.add(fee);

        return new PaymentCalculation(fee, finalTotal, 1, finalTotal);
    }

    private PaymentCalculation calculateCreditCard(BigDecimal orderTotal, int installments) {
        if (installments <= 3) {
            BigDecimal installmentAmount = orderTotal.divide(new BigDecimal(installments), 2, RoundingMode.HALF_EVEN);
            return new PaymentCalculation(BigDecimal.ZERO, orderTotal, installments, installmentAmount);
        }

        BigDecimal monthlyRate = new BigDecimal("0.0199");
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                onePlusRate.pow(-installments)
        );

        BigDecimal installmentAmount = orderTotal.multiply(monthlyRate)
                .divide(denominator, 2, RoundingMode.HALF_EVEN);

        BigDecimal finalTotal = installmentAmount.multiply(new BigDecimal(installments));
        BigDecimal adjustment = finalTotal.subtract(orderTotal);

        return new PaymentCalculation(adjustment, finalTotal, installments, installmentAmount);
    }
}
