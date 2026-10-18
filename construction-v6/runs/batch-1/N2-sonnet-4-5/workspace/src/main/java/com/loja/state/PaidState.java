package com.loja.state;

import com.loja.domain.Order;
import com.loja.domain.OrderPayment;
import com.loja.dto.PaymentRequest;
import com.loja.dto.ShipmentRequest;
import com.loja.exception.OrderException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PaidState implements OrderState {
    @Override
    public String getStatusCode() {
        return "PAID";
    }

    @Override
    public void handlePayment(Order order, PaymentRequest paymentRequest, OrderStateContext context) {
        OrderPayment existingPayment = context.findPaymentByOrderId(order.getId());
        if (existingPayment != null && existingPayment.getTransactionId().equals(paymentRequest.getTransactionId())) {
            return;
        }
        throw new OrderException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
    }

    @Override
    public void handleShipment(Order order, ShipmentRequest shipmentRequest, OrderStateContext context) {
        order.setTrackingCode(shipmentRequest.getTrackingCode());
        order.setStatus("SHIPPED");
        order.setState(new ShippedState());
        context.saveOrder(order);
        context.notifyCustomer(order.getCustomerCpf(), "ORDER_SHIPPED", order.getId());
    }

    @Override
    public void handleDelivery(Order order, OrderStateContext context) {
        throw new OrderException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
    }

    @Override
    public void handleCancellation(Order order, OrderStateContext context) {
        context.releaseStock(order);
        context.removeCredit(order.getCustomerCpf(), order.getNextPurchaseCredit());

        order.setStatus("CANCELLED");
        order.setState(new CancelledState());
        context.saveOrder(order);
        context.notifyCustomer(order.getCustomerCpf(), "ORDER_CANCELLED", order.getId());
    }
}
