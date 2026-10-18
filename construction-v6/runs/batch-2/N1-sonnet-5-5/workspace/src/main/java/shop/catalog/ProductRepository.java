package shop.catalog;

import java.util.Optional;

public interface ProductRepository {
    Optional<Product> find(String sku);

    /** Reads the product locking it until the end of the transaction. */
    Optional<Product> lock(String sku);

    void adjustStock(String sku, int delta);

    /** Used to load the initial catalog; never overwrites an existing product. */
    void insertIfAbsent(Product product);
}
