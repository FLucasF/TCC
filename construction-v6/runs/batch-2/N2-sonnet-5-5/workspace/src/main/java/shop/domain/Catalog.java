package shop.domain;

import java.math.BigDecimal;
import java.util.List;

public final class Catalog {
    private Catalog() {
    }

    public static List<Product> initial() {
        return List.of(
                product("CAM-001", "Camiseta básica", "79.90", "0.30", 50, false),
                product("TEN-001", "Tênis corrida", "249.90", "1.20", 10, false),
                product("FON-001", "Fone sem fio", "199.90", "0.25", 5, false),
                product("MEI-001", "Meia esportiva", "19.90", "0.10", 100, false),
                product("JAQ-001", "Jaqueta corta-vento", "389.90", "0.80", 3, false),
                product("MOC-001", "Mochila urbana", "159.90", "0.90", 1, false),
                product("BON-001", "Boné aba reta", "59.90", "0.15", 0, false),
                product("REL-001", "Relógio digital", "129.90", "0.20", 8, true));
    }

    private static Product product(String sku, String name, String price, String weight, int stock,
                                   boolean discontinued) {
        return new Product(sku, name, new BigDecimal(price), new BigDecimal(weight), stock, discontinued);
    }
}
