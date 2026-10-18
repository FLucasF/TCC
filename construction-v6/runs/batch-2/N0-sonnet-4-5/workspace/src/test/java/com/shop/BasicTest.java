package com.shop;

import com.shop.service.PaymentService;
import com.shop.model.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BasicTest {

    @Test
    void testPaymentServiceExists() {
        PaymentService service = new PaymentService();
        assertNotNull(service);
    }

    @Test
    void testPixDiscount() {
        PaymentService service = new PaymentService();
        PaymentService.PaymentCalculation result = service.calculatePayment(
                PaymentMethod.PIX,
                new BigDecimal("100.00"),
                1,
                null,
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("95.00"), result.finalTotal);
        assertEquals(new BigDecimal("-5.00"), result.adjustment);
    }

    @Test
    void testBankSlipFee() {
        PaymentService service = new PaymentService();
        PaymentService.PaymentCalculation result = service.calculatePayment(
                PaymentMethod.BANK_SLIP,
                new BigDecimal("100.00"),
                1,
                null,
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("103.49"), result.finalTotal);
        assertEquals(new BigDecimal("3.49"), result.adjustment);
    }

    @Test
    void testCreditCardNoInterest() {
        PaymentService service = new PaymentService();
        PaymentService.PaymentCalculation result = service.calculatePayment(
                PaymentMethod.CREDIT_CARD,
                new BigDecimal("300.00"),
                3,
                null,
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("300.00"), result.finalTotal);
        assertEquals(new BigDecimal("100.00"), result.installmentAmount);
        assertEquals(3, result.installments);
    }
}
