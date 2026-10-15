package reference;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The product catalog, with the stock at a given moment. The initial one is the table in the prompt's annex. */
public final class Catalog {

    public record Product(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock, boolean discontinued) {

        /** The same product, with another stock. */
        public Product withStock(int newStock) {
            return new Product(sku, name, price, weightKg, newStock, discontinued);
        }
    }

    private final Map<String, Product> products = new LinkedHashMap<>();

    public Catalog(List<Product> products) {
        products.forEach(p -> this.products.put(p.sku(), p));
    }

    public Optional<Product> find(String sku) {
        return Optional.ofNullable(products.get(sku));
    }

    public List<Product> all() {
        return List.copyOf(products.values());
    }

    /** The initial catalog, exactly as in the table of the prompt's annex. */
    public static Catalog initial() {
        return new Catalog(List.of(
                product("CAM-001", "Camiseta básica", "79.90", "0.30", 50, false),
                product("TEN-001", "Tênis corrida", "249.90", "1.20", 10, false),
                product("FON-001", "Fone sem fio", "199.90", "0.25", 5, false),
                product("MEI-001", "Meia esportiva", "19.90", "0.10", 100, false),
                product("JAQ-001", "Jaqueta corta-vento", "389.90", "0.80", 3, false),
                product("MOC-001", "Mochila urbana", "159.90", "0.90", 1, false),
                product("BON-001", "Boné aba reta", "59.90", "0.15", 0, false),
                product("REL-001", "Relógio digital", "129.90", "0.20", 8, true)));
    }

    public static Product product(String sku, String name, String price, String weightKg, int stock, boolean discontinued) {
        return new Product(sku, name, new BigDecimal(price), new BigDecimal(weightKg), stock, discontinued);
    }
}
