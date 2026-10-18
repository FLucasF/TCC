package com.loja.domain.observers;

import com.loja.domain.entities.Order;
import com.loja.domain.entities.OrderItem;
import com.loja.domain.entities.Product;
import com.loja.domain.events.PaymentConfirmedEvent;
import com.loja.repositories.ProductRepository;

public class StockObserver implements PaymentConfirmedObserver {
    private final ProductRepository productRepository;

    public StockObserver(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void onPaymentConfirmed(PaymentConfirmedEvent event) throws Exception {
        Order order = event.getOrder();

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySku(item.getSku());
            if (!product.hasStock(item.getQuantity())) {
                throw new InsufficientStockException("Insufficient stock for product " + item.getSku());
            }
        }

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySku(item.getSku());
            product.decreaseStock(item.getQuantity());
            productRepository.update(product);
        }
    }

    public static class InsufficientStockException extends Exception {
        public InsufficientStockException(String message) {
            super(message);
        }
    }
}
