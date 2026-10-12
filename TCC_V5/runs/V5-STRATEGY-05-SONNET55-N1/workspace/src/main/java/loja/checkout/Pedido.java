package loja.checkout;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho já validado, com os totais que todas as regras consultam. */
public record Pedido(List<Item> itens, BigDecimal subtotal, BigDecimal pesoKg) {

    public static Pedido de(List<Item> itens) {
        BigDecimal subtotal = itens.stream().map(Item::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Pedido(itens, Dinheiro.arredondar(subtotal), peso);
    }
}
