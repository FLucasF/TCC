package com.loja.orders.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.loja.orders.order.OrderItem;

/**
 * Baixa e devolução de estoque. As linhas dos produtos ficam travadas até o fim da transação, então
 * dois pagamentos simultâneos da última peça nunca baixam o mesmo estoque duas vezes.
 */
@Service
public class InventoryService {

    private final ProductRepository products;

    public InventoryService(ProductRepository products) {
        this.products = products;
    }

    /** Baixa o estoque de todos os itens, ou de nenhum se faltar algum. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean take(List<OrderItem> items) {
        List<OrderItem> sorted = sortedBySku(items);
        Map<String, Integer> stock = products.lockStock(sorted.stream().map(OrderItem::sku).toList());
        boolean available = sorted.stream()
                .allMatch(item -> stock.getOrDefault(item.sku(), 0) >= item.quantity());
        if (!available) {
            return false;
        }
        sorted.forEach(item -> products.changeStock(item.sku(), -item.quantity()));
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void giveBack(List<OrderItem> items) {
        List<OrderItem> sorted = sortedBySku(items);
        products.lockStock(sorted.stream().map(OrderItem::sku).toList());
        sorted.forEach(item -> products.changeStock(item.sku(), item.quantity()));
    }

    private static List<OrderItem> sortedBySku(List<OrderItem> items) {
        return items.stream().sorted(Comparator.comparing(OrderItem::sku)).toList();
    }
}
