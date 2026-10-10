package loja.checkout.comum;

import java.math.BigDecimal;
import java.util.List;

public record Compra(List<Item> itens, BigDecimal subtotal, BigDecimal pesoKg) {
    public static Compra de(List<Item> itens) {
        BigDecimal subtotal = itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Compra(itens, Dinheiro.arredondar(subtotal), peso);
    }
}
