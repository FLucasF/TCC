package com.loja.state;

import com.loja.domain.Order;
import com.loja.domain.OrderPayment;
import com.loja.dto.PaymentRequest;
import java.util.List;

public interface OrderStateContext {
    void saveOrder(Order order);
    void reserveStock(Order order);
    void releaseStock(Order order);
    void addCredit(String cpf, java.math.BigDecimal amount);
    void removeCredit(String cpf, java.math.BigDecimal amount);
    void notifyCustomer(String cpf, String type, String orderId);
    OrderPayment findPaymentByOrderId(String orderId);
    void savePayment(OrderPayment payment);
    List<Order> findOrdersByCustomerCpfAndStatuses(String cpf, List<String> statuses);
    boolean isPaymentExpired(Order order, PaymentRequest paymentRequest);
    boolean hasStockForOrder(Order order);
}
