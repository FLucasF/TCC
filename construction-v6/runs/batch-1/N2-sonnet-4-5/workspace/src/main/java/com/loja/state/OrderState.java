package com.loja.state;

import com.loja.domain.Order;
import com.loja.dto.PaymentRequest;
import com.loja.dto.ShipmentRequest;

public interface OrderState {
    String getStatusCode();
    void handlePayment(Order order, PaymentRequest paymentRequest, OrderStateContext context);
    void handleShipment(Order order, ShipmentRequest shipmentRequest, OrderStateContext context);
    void handleDelivery(Order order, OrderStateContext context);
    void handleCancellation(Order order, OrderStateContext context);
}
