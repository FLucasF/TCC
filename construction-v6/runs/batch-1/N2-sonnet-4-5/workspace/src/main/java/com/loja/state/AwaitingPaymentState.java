package com.loja.state;

import com.loja.domain.Order;
import com.loja.domain.OrderPayment;
import com.loja.dto.PaymentRequest;
import com.loja.dto.ShipmentRequest;
import com.loja.exception.OrderException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AwaitingPaymentState implements OrderState {
    @Override
    public String getStatusCode() {
        return "AWAITING_PAYMENT";
    }

    @Override
    public void handlePayment(Order order, PaymentRequest paymentRequest, OrderStateContext context) {
        OrderPayment existingPayment = context.findPaymentByOrderId(order.getId());
        if (existingPayment != null) {
            if (existingPayment.getTransactionId().equals(paymentRequest.getTransactionId())) {
                return;
            }
            throw new OrderException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
        }

        if (context.isPaymentExpired(order, paymentRequest)) {
            order.setStatus("CANCELLED");
            order.setState(new CancelledState());
            context.saveOrder(order);
            context.notifyCustomer(order.getCustomerCpf(), "ORDER_CANCELLED", order.getId());
            throw new OrderException("ORDER_EXPIRED", HttpStatus.CONFLICT);
        }

        if (!context.hasStockForOrder(order)) {
            order.setStatus("CANCELLED");
            order.setState(new CancelledState());
            context.saveOrder(order);
            context.notifyCustomer(order.getCustomerCpf(), "ORDER_CANCELLED", order.getId());
            throw new OrderException("INSUFFICIENT_STOCK", HttpStatus.CONFLICT);
        }

        context.savePayment(new OrderPayment(order.getId(), paymentRequest.getTransactionId(), paymentRequest.getPaidAt()));

        context.reserveStock(order);
        context.addCredit(order.getCustomerCpf(), order.getNextPurchaseCredit());
        context.notifyCustomer(order.getCustomerCpf(), "PAYMENT_APPROVED", order.getId());

        order.setStatus("PAID");
        order.setState(new PaidState());
        context.saveOrder(order);
    }

    @Override
    public void handleShipment(Order order, ShipmentRequest shipmentRequest, OrderStateContext context) {
        throw new OrderException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
    }

    @Override
    public void handleDelivery(Order order, OrderStateContext context) {
        throw new OrderException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
    }

    @Override
    public void handleCancellation(Order order, OrderStateContext context) {
        order.setStatus("CANCELLED");
        order.setState(new CancelledState());
        context.saveOrder(order);
        context.notifyCustomer(order.getCustomerCpf(), "ORDER_CANCELLED", order.getId());
    }
}
