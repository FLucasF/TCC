package shop.persistence;

import java.util.List;
import java.util.Optional;
import shop.domain.CartItem;
import shop.domain.Product;

public interface ProductRepository {
    Optional<Product> find(String sku);

    void insertIfAbsent(Product product);

    /** Baixa o estoque de todos os itens ou de nenhum; false (sem alterar nada) se algum não tem estoque. */
    boolean tryDebit(List<CartItem> items);

    void restore(List<CartItem> items);
}
