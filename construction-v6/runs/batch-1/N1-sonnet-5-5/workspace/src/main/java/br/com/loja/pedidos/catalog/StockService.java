package br.com.loja.pedidos.catalog;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.order.OrderItem;

/** Deve ser chamado dentro de uma transação. */
@Service
public class StockService {

    private final ProductRepository products;

    public StockService(ProductRepository products) {
        this.products = products;
    }

    /** Baixa o estoque de todos os itens ou de nenhum; lança INSUFFICIENT_STOCK se algum faltar. */
    public void reserve(List<OrderItem> items) {
        // ordem fixa de SKU nas travas evita deadlock entre pagamentos simultâneos
        List<OrderItem> sorted = items.stream().sorted(Comparator.comparing(OrderItem::sku)).toList();
        for (OrderItem item : sorted) {
            int stock = products.lockStock(item.sku()).orElse(0);
            if (stock < item.quantity()) {
                throw DomainException.conflict("INSUFFICIENT_STOCK");
            }
        }
        sorted.forEach(item -> products.addStock(item.sku(), -item.quantity()));
    }

    public void release(List<OrderItem> items) {
        items.forEach(item -> products.addStock(item.sku(), item.quantity()));
    }
}
